package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.entities.contracts.IAudioEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.mapObjectId
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.convertToProps
import com.megaman.maverick.game.world.body.getCenter
import kotlin.reflect.KClass

/**
 * Spawns waves of enemies in one room. The next wave spawns only when all enemies of the current wave are dead. When
 * the last wave is dead, a [EventType.GAUNTLET_CLEARED] event is submitted with this master's key. A [com.megaman
 * .maverick.game.entities.sensors.Gate] with the same `gauntlet_key` is then unlocked.
 *
 * TMX properties:
 * - `spawn_type`: must be `spawn_now`
 * - `room`: the name of the room that this master controls
 * - `key`: the gauntlet key; must match the `gauntlet_key` of the room's exit gate
 * - `wave_<w>_<n>`: object ref to enemy `n` of wave `w` (both start at 1); the referenced object must be in a layer
 *   that is not built (e.g. `children`), its name is the enemy class name, and its custom props go to the enemy
 * - `start_delay` (optional): seconds before wave 1 after the player enters the room
 * - `wave_delay` (optional): seconds between waves
 */
class GauntletMaster(game: MegaServices) : MegaGameEntity(game), IEventListener, IAudioEntity {

    companion object {
        const val TAG = "GauntletMaster"

        private const val DEFAULT_START_DELAY = 1.5f
        private const val DEFAULT_WAVE_DELAY = 1f

        // An enemy farther than this outside the room bounds is destroyed so that the gauntlet cannot soft-lock
        private const val OUT_OF_ROOM_MARGIN = 2f

        private val WAVE_KEY_REGEX = Regex("${ConstKeys.WAVE}_(\\d+)_(\\d+)")
    }

    private enum class GauntletState { INACTIVE, STARTING, WAVE_ACTIVE, WAVE_DELAY, CLEARED }

    private data class WaveEntry(val clazz: KClass<out MegaGameEntity>, val mapObject: RectangleMapObject)

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_JUST_DIED,
        EventType.BEGIN_ROOM_TRANS,
        EventType.END_ROOM_TRANS,
        EventType.SET_TO_ROOM_NO_TRANS
    )

    private val waves = Array<Array<WaveEntry>>()
    private val children = Array<MegaGameEntity>()
    // Dead children whose destroy runnable must still be removed. The runnable is not removed inside itself, because
    // that changes the runnables map while it is iterated.
    private val deadChildren = Array<MegaGameEntity>()
    private val roomBounds = GameRectangle()

    private lateinit var room: String
    private lateinit var key: String
    private lateinit var childRunnableKey: String

    private val startTimer = Timer(DEFAULT_START_DELAY)
    private val waveTimer = Timer(DEFAULT_WAVE_DELAY)

    private var state = GauntletState.INACTIVE
    private var waveIndex = 0

    override fun getType() = EntityType.SPECIAL

    override fun init(vararg params: Any) {
        addComponent(defineUpdatablesComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        room = spawnProps.get(ConstKeys.ROOM, String::class)!!
        key = spawnProps.get(ConstKeys.KEY, String::class)!!
        childRunnableKey = "${TAG}_${key}"

        startTimer.resetDuration(
            spawnProps.getOrDefault("${ConstKeys.START}_${ConstKeys.DELAY}", DEFAULT_START_DELAY, Float::class)
        )
        waveTimer.resetDuration(
            spawnProps.getOrDefault("${ConstKeys.WAVE}_${ConstKeys.DELAY}", DEFAULT_WAVE_DELAY, Float::class)
        )

        parseWaves(spawnProps)

        resetGauntlet()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy(): key=$key")
        super.onDestroy()

        game.eventsMan.removeListener(this)

        resetGauntlet()
        waves.clear()
    }

    private fun parseWaves(spawnProps: Properties) {
        waves.clear()

        val entries = Array<Triple<Int, Int, WaveEntry>>()
        spawnProps.forEach { propKey, value ->
            val match = WAVE_KEY_REGEX.matchEntire(propKey.toString()) ?: return@forEach

            if (value !is RectangleMapObject) {
                val childId = if (value is MapObject) value.properties.get(ConstKeys.ID) else value
                throw IllegalStateException(
                    "Gauntlet $key (map object id=$mapObjectId): prop $propKey must be an object ref to a rectangle " +
                        "object, but the ref is to child id=$childId (type=${value?.javaClass?.simpleName}). " +
                        "Check that the child object exists and is a rectangle."
                )
            }

            val clazz = MegaEntityFactory.getEntityClass(EntityType.ENEMY, value.name)

            val waveNumber = match.groupValues[1].toInt()
            val enemyNumber = match.groupValues[2].toInt()
            entries.add(Triple(waveNumber, enemyNumber, WaveEntry(clazz, value)))
        }

        if (entries.isEmpty) throw IllegalStateException("Gauntlet $key has no waves")

        entries.sort { a, b -> if (a.first != b.first) a.first - b.first else a.second - b.second }

        entries.forEach { (waveNumber, _, entry) ->
            if (waveNumber < 1 || waveNumber > waves.size + 1) throw IllegalStateException(
                "Gauntlet $key: wave numbers must start at 1 and have no gaps, found wave $waveNumber"
            )

            if (waveNumber > waves.size) waves.add(Array())
            waves[waveNumber - 1].add(entry)
        }

        GameLogger.debug(TAG, "parseWaves(): key=$key, waves=${waves.size}")
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.PLAYER_JUST_DIED -> {
                GameLogger.debug(TAG, "onEvent(): key=$key, PLAYER_JUST_DIED")
                resetGauntlet()
            }

            EventType.BEGIN_ROOM_TRANS -> if (state != GauntletState.CLEARED) {
                GameLogger.debug(TAG, "onEvent(): key=$key, BEGIN_ROOM_TRANS")
                resetGauntlet()
            }

            EventType.END_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS -> {
                val roomObject = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!
                if (roomObject.name == room && state == GauntletState.INACTIVE) {
                    GameLogger.debug(TAG, "onEvent(): key=$key, entered room $room: start gauntlet")
                    roomBounds.set(roomObject.rectangle)
                    startTimer.reset()
                    state = GauntletState.STARTING
                }
            }
        }
    }

    private fun resetGauntlet() {
        // Remove the runnables before the children are destroyed by the level screen, so that their deaths do not
        // count as kills
        children.forEach { it.runnablesOnDestroy.remove(childRunnableKey) }
        children.clear()
        removeDeadChildrenRunnables()

        startTimer.reset()
        waveTimer.reset()

        state = GauntletState.INACTIVE
        waveIndex = 0
    }

    private fun spawnWave() {
        val wave = waves[waveIndex]

        GameLogger.debug(TAG, "spawnWave(): key=$key, wave=${waveIndex + 1}/${waves.size}, enemies=${wave.size}")

        wave.forEach { (clazz, mapObject) ->
            val enemy = MegaEntityFactory.fetch(clazz) ?: throw IllegalStateException(
                "Entity of type ${EntityType.ENEMY} not found: ${mapObject.name}"
            )

            val enemyProps = mapObject.convertToProps()
            enemyProps.putAll(
                props(
                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                    ConstKeys.CULL_EVENTS pairTo false
                )
            )

            if (!enemy.spawn(enemyProps)) {
                GameLogger.error(TAG, "spawnWave(): key=$key, spawn refused: ${mapObject.name}")
                return@forEach
            }

            enemy.runnablesOnDestroy.put(childRunnableKey) {
                if (children.removeValue(enemy, true)) deadChildren.add(enemy)
            }
            children.add(enemy)
        }

        state = GauntletState.WAVE_ACTIVE
    }

    private fun removeDeadChildrenRunnables() {
        deadChildren.forEach { it.runnablesOnDestroy.remove(childRunnableKey) }
        deadChildren.clear()
    }

    private fun destroyChildrenOutOfRoom() {
        val margin = OUT_OF_ROOM_MARGIN * ConstVals.PPM

        val iter = children.iterator()
        while (iter.hasNext()) {
            val child = iter.next()
            if (child !is IBodyEntity) continue

            val center = child.body.getCenter()
            val outOfRoom = center.x < roomBounds.getX() - margin ||
                center.x > roomBounds.getMaxX() + margin ||
                center.y < roomBounds.getY() - margin ||
                center.y > roomBounds.getMaxY() + margin

            if (outOfRoom) {
                GameLogger.debug(TAG, "destroyChildrenOutOfRoom(): key=$key, destroy $child")
                child.runnablesOnDestroy.remove(childRunnableKey)
                iter.remove()
                child.destroy()
            }
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        removeDeadChildrenRunnables()

        when (state) {
            GauntletState.INACTIVE, GauntletState.CLEARED -> {}

            GauntletState.STARTING -> {
                startTimer.update(delta)
                if (startTimer.isFinished()) spawnWave()
            }

            GauntletState.WAVE_ACTIVE -> {
                destroyChildrenOutOfRoom()

                if (children.isEmpty) {
                    waveIndex++

                    if (waveIndex < waves.size) {
                        GameLogger.debug(TAG, "update(): key=$key, wave $waveIndex done")
                        waveTimer.reset()
                        state = GauntletState.WAVE_DELAY
                    } else {
                        GameLogger.debug(TAG, "update(): key=$key, CLEARED")
                        state = GauntletState.CLEARED
                        requestToPlaySound(SoundAsset.SELECT_PING_SOUND, false)
                        game.eventsMan.submitEvent(
                            Event(EventType.GAUNTLET_CLEARED, props(ConstKeys.KEY pairTo key))
                        )
                    }
                }
            }

            GauntletState.WAVE_DELAY -> {
                waveTimer.update(delta)
                if (waveTimer.isFinished()) spawnWave()
            }
        }
    })
}
