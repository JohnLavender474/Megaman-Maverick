package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.random
import com.mega.game.engine.common.interfaces.IActivatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.IParentEntity
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
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.getCenter

class AsteroidsSpawner(game: MegamanMaverickGame) : MegaGameEntity(game), IParentEntity, ICullableEntity,
    IDrawableShapesEntity, IActivatable {

    companion object {
        const val TAG = "AsteroidsSpawner"

        const val MIN_SPEED = 1.5f
        const val MAX_SPEED = 3f

        private const val MIN_SPAWN_DELAY = 2f
        private const val MAX_SPAWN_DELAY = 4f

        private const val MIN_ANGLE = 240f
        private const val MAX_ANGLE = 300f

        private const val MAX_CHILDREN = 4
    }

    override var children = Array<IGameEntity>()
    override var on = true
        set(value) {
            field = value
            if (value) resetSpawnTimer()
        }

    private val bounds = GameRectangle()

    private val spawnTimer = Timer()
    private var spawnRoom: String? = null
    var onSpawnListener: ((Asteroid) -> Unit)? = null

    private var cullOOBChildren = true
    private var destroyChildren = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ bounds }), debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)

        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        if (cullOutOfBounds) putCullable(
            ConstKeys.CULL_OUT_OF_BOUNDS,
            getGameCameraCullingLogic(getGameCamera(), { bounds })
        ) else removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)

        on = spawnProps.getOrDefault(ConstKeys.ON, true, Boolean::class)

        resetSpawnTimer()
        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)
        onSpawnListener = spawnProps.get(ConstKeys.LISTENER) as ((Asteroid) -> Unit)?

        destroyChildren = spawnProps.getOrDefault("${ConstKeys.DESTROY}_${ConstKeys.CHILDREN}", false, Boolean::class)
        cullOOBChildren = spawnProps.getOrDefault(
            "${ConstKeys.CULL_OUT_OF_BOUNDS}_${ConstKeys.CHILDREN}", true, Boolean::class
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        if (destroyChildren) children.forEach { (it as MegaGameEntity).destroy() }
        children.clear()
    }

    private fun resetSpawnTimer() {
        GameLogger.debug(TAG, "resetSpawnTimer()")
        val newDuration = getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY)
        spawnTimer.resetDuration(newDuration)
    }

    private fun shouldAimAtMoonEyeStone() = UtilMethods.getRandomBool() &&
        MegaGameEntities.getOfTag(MoonEyeStone.TAG).size > 0

    private fun getRandomMoonEyeStone() = MegaGameEntities.getOfTag(MoonEyeStone.TAG).random() as MoonEyeStone

    private fun spawnAsteroid() {
        val spawn = GameObjectPools.fetch(Vector2::class).set(
            getRandom(bounds.getX(), bounds.getMaxX()), bounds.getMaxY()
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
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOOBChildren
            )
        )
        children.add(asteroid)

        GameLogger.debug(TAG, "Spawned asteroid. Size of children: ${children.size}")
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(update@{ delta ->
        children.removeAll { (it as MegaGameEntity).dead }

        if (!on || children.size >= MAX_CHILDREN || game.isProperty(ConstKeys.ROOM_TRANSITION, true)) {
            spawnTimer.setToEnd()
            return@update
        }

        spawnTimer.update(delta)
        if (spawnTimer.isFinished()) {
            spawnAsteroid()
            resetSpawnTimer()
        }
    })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this,
                objectSetOf(EventType.BEGIN_ROOM_TRANS),
                cull@{ event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    return@cull room != spawnRoom
                }
            )
        )
    )

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
