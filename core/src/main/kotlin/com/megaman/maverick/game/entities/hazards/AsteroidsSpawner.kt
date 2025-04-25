package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
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
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.projectiles.Asteroid
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.utils.GameObjectPools

class AsteroidsSpawner(game: MegamanMaverickGame) : MegaGameEntity(game), IParentEntity, ICullableEntity,
    IDrawableShapesEntity, IActivatable {

    companion object {
        const val TAG = "AsteroidsSpawner"

        const val MIN_SPEED = 2f
        const val MAX_SPEED = 4f

        private const val MIN_SPAWN_DELAY = 0.75f
        private const val MAX_SPAWN_DELAY = 1.5f

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

    var onSpawnListener: ((Asteroid) -> Unit)? = null

    private val bounds = GameRectangle()
    private lateinit var spawnTimer: Timer
    private var destroyChildren = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
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

        spawnTimer = Timer()
        resetSpawnTimer()

        on = spawnProps.getOrDefault(ConstKeys.ON, true, Boolean::class)
        onSpawnListener = spawnProps.get(ConstKeys.LISTENER) as ((Asteroid) -> Unit)?
        destroyChildren = spawnProps.getOrDefault("${ConstKeys.DESTROY}_${ConstKeys.CHILDREN}", false, Boolean::class)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (destroyChildren) children.forEach { (it as MegaGameEntity).destroy() }
        children.clear()
    }

    private fun resetSpawnTimer() {
        val newDuration = getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY)
        spawnTimer.resetDuration(newDuration)
    }

    private fun spawnAsteroid() {
        val angle = getRandom(MIN_ANGLE, MAX_ANGLE)
        val speed = getRandom(MIN_SPEED, MAX_SPEED)

        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(speed, 0f)
            .rotateDeg(angle).scl(ConstVals.PPM.toFloat())

        val posX = getRandom(bounds.getX(), bounds.getMaxX())
        val posY = bounds.getMaxY()

        val asteroid = MegaEntityFactory.fetch(Asteroid::class)!!
        asteroid.spawn(
            props(
                ConstKeys.POSITION pairTo Vector2(posX, posY),
                ConstKeys.IMPULSE pairTo impulse
            )
        )

        children.add(asteroid)

        GameLogger.debug(TAG, "Spawned asteroid. Size of children: ${children.size}")
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        children.removeAll { (it as MegaGameEntity).dead }

        val roomTrans = game.isProperty(ConstKeys.ROOM_TRANSITION, true)
        if (children.size >= MAX_CHILDREN || roomTrans || !on) return@UpdatablesComponent

        spawnTimer.update(delta)
        if (spawnTimer.isFinished()) {
            spawnAsteroid()
            resetSpawnTimer()
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOnEvents = getStandardEventCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_EVENTS pairTo cullOnEvents))
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
