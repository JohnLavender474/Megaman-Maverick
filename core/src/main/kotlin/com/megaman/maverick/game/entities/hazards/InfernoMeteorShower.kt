package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.projectiles.MagmaMeteor
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.levels.LevelUtils
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.toGameRectangle

class InfernoMeteorShower(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity, IDirectional {

    companion object {
        const val TAG = "InfernoMeteorShower"

        private const val COOLDOWN_DUR = 2f
        private const val METEOR_SHOWER_DUR = 4f

        private const val INIT_SPAWN_DELAY = 0.5f
        private const val MIN_CONTINUE_SPAWN_DELAY = 0.25f
        private const val MAX_CONTINUE_SPAWN_DELAY = 0.5f

        private const val SHAKE_DUR = 4f
        private const val SHAKE_INTERVAL = 0.1f
        private const val SHAKE_X = 0f
        private const val SHAKE_Y = 0.003125f

        private const val METEOR_CULL_TIME = 3f
    }

    override lateinit var direction: Direction

    private val bounds = GameRectangle()

    private val spawners = OrderedMap<GameRectangle, Timer>()
    private val timers = OrderedMap<String, Timer>()

    private lateinit var spawnRoom: String

    private var coolingDown = true

    override fun init() {
        if (timers.isEmpty) {
            timers.put("cooldown", Timer(COOLDOWN_DUR))
            timers.put("meteor_shower", Timer(METEOR_SHOWER_DUR))
            timers.put("init_spawn_delay", Timer(INIT_SPAWN_DELAY))
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun canSpawn(spawnProps: Properties) = !LevelUtils.isInfernoManLevelFrozen(game.state)

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        this.bounds.set(bounds)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.SPAWNER)) {
                val spawner = (value as RectangleMapObject).rectangle.toGameRectangle(false)
                spawners.put(spawner, Timer(UtilMethods.getRandom(MIN_CONTINUE_SPAWN_DELAY, MAX_CONTINUE_SPAWN_DELAY)))
            }
        }

        direction = Direction.valueOf(
            spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.DOWN, String::class).uppercase()
        )

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
        timers.values().forEach { it.reset() }
        coolingDown = true
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        spawners.clear()
    }

    private fun spawnMeteor(spawn: Vector2) {
        GameLogger.debug(TAG, "spawnMeteor(): spawn=$spawn")

        val meteor = MegaEntityFactory.fetch(MagmaMeteor::class)!!
        meteor.spawn(
            props(
                ConstKeys.DIRECTION pairTo direction,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.CULL_TIME pairTo METEOR_CULL_TIME
            )
        )
    }

    private fun shakeRoom() {
        GameLogger.debug(TAG, "shakeRoom()")

        game.eventsMan.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.INTERVAL pairTo SHAKE_INTERVAL,
                    ConstKeys.DURATION pairTo SHAKE_DUR,
                    ConstKeys.X pairTo SHAKE_X * ConstVals.PPM,
                    ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
                )
            )
        )
    }

    private fun resetTimers() {
        timers.values().forEach { it.reset() }
        spawners.values().forEach { it.reset() }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val cooldown = timers["cooldown"]

        when {
            coolingDown -> {
                cooldown.update(delta)

                if (cooldown.isFinished()) {
                    coolingDown = false

                    resetTimers()

                    val overlap = spawners.keys().any { it.overlaps(game.getGameCamera().toGameRectangle()) }
                    if (overlap) shakeRoom()
                }
            }
            else -> {
                val initSpawnDelay = timers["init_spawn_delay"]
                initSpawnDelay.update(delta)

                if (initSpawnDelay.isFinished()) spawners.forEach { entry ->
                    val spawner = entry.key
                    val timer = entry.value

                    timer.update(delta)
                    if (timer.isFinished()) {
                        val x = UtilMethods.getRandom(spawner.getX(), spawner.getMaxX())
                        val y = spawner.getMaxY()
                        val spawn = GameObjectPools.fetch(Vector2::class).set(x, y)

                        spawnMeteor(spawn)

                        timer.resetDuration(
                            UtilMethods.getRandom(MIN_CONTINUE_SPAWN_DELAY, MAX_CONTINUE_SPAWN_DELAY)
                        )
                    }
                }

                val meteorShower = timers["meteor_shower"]
                meteorShower.update(delta)

                if (meteorShower.isFinished()) {
                    coolingDown = true
                    resetTimers()
                }
            }
        }
    })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS), event@{ event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    val cull = room != spawnRoom
                    GameLogger.debug(
                        TAG,
                        "defineCullablesComponent(): currentRoom=$room, spawnRoom=$spawnRoom, cull=$cull"
                    )
                    return@event cull
                }
            )
        )
    )

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
