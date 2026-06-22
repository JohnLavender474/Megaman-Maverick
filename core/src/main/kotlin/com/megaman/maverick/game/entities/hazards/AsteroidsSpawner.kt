package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.random
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.MoonEyeStone
import com.megaman.maverick.game.entities.projectiles.Asteroid
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.getCenter

class AsteroidsSpawner(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity {

    companion object {
        const val TAG = "AsteroidsSpawner"

        const val MIN_SPEED = 3f
        const val MAX_SPEED = 4f

        private const val MIN_SPAWN_DELAY = 1.25f
        private const val MAX_SPAWN_DELAY = 2.5f

        private const val MIN_ANGLE = 240f
        private const val MAX_ANGLE = 300f

        private const val DEFAULT_MIN_Y = -10f * ConstVals.PPM

        private const val CAMERA_MAX_DIST_X = 10f
    }

    private val spawners = OrderedMap<GameRectangle, Timer>()

    private var spawnRoom: String? = null
    var onSpawnListener: ((Asteroid) -> Unit)? = null

    private var minY = DEFAULT_MIN_Y

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.SPAWNER)) {
                val spawner = (value as RectangleMapObject).rectangle.toGameRectangle(false)
                spawners.put(spawner, Timer(getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY)))
            }
        }

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)
        onSpawnListener = spawnProps.get(ConstKeys.LISTENER) as ((Asteroid) -> Unit)?

        val minYObj = spawnProps.get("${ConstKeys.MIN}_${ConstKeys.Y}", RectangleMapObject::class)
        minY = if (minYObj != null) minYObj.rectangle.y else DEFAULT_MIN_Y
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        spawners.clear()
    }

    private fun shouldAimAtMoonEyeStone() = UtilMethods.getRandomBool() &&
        MegaGameEntities.getOfTag(MoonEyeStone.TAG).size > 0

    private fun getRandomMoonEyeStone() = MegaGameEntities.getOfTag(MoonEyeStone.TAG).random() as MoonEyeStone

    private fun spawnAsteroid(spawner: GameRectangle) {
        val spawn = GameObjectPools.fetch(Vector2::class).set(
            getRandom(spawner.getX(), spawner.getMaxX()), spawner.getMaxY()
        )

        val angle = when {
            shouldAimAtMoonEyeStone() -> getRandomMoonEyeStone()
                .body
                .getCenter()
                .sub(spawn)
                .angleDeg()
                .coerceIn(MIN_ANGLE, MAX_ANGLE)

            else -> getRandom(MIN_ANGLE, MAX_ANGLE)
        }

        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(getRandom(MIN_SPEED, MAX_SPEED), 0f)
            .rotateDeg(angle).scl(ConstVals.PPM.toFloat())

        val asteroid = MegaEntityFactory.fetch(Asteroid::class)!!
        asteroid.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse,
                "${ConstKeys.MINI}_${ConstKeys.Y}" pairTo minY
            )
        )
        onSpawnListener?.invoke(asteroid)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(update@{ delta ->
        if (game.isProperty(ConstKeys.ROOM_TRANSITION, true)) return@update

        val camBounds = game.getGameCamera().getRotatedBounds()
        spawners.forEach spawner@{ entry ->
            val spawner = entry.key
            if (spawner.getX() > camBounds.getMaxX() + CAMERA_MAX_DIST_X * ConstVals.PPM ||
                spawner.getMaxX() < camBounds.getX() - CAMERA_MAX_DIST_X * ConstVals.PPM
            ) return@spawner

            val timer = entry.value
            timer.update(delta)
            if (timer.isFinished()) {
                spawnAsteroid(spawner)
                timer.resetDuration(getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY))
            }
        }
    })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this,
                objectSetOf(EventType.END_ROOM_TRANS, EventType.PLAYER_SPAWN),
                { event ->
                    val cull = when (event.key) {
                        EventType.PLAYER_SPAWN -> true
                        EventType.END_ROOM_TRANS -> {
                            val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                            room != spawnRoom
                        }
                        else -> false
                    }
                    GameLogger.debug(
                        TAG,
                        "defineCullablesComponent(): event=${event.key}, spawnRoom=$spawnRoom, cull=$cull"
                    )
                    cull
                }
            )
        )
    )

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
