package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.calculateAngleDegrees
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.processAndFilter
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.RotatingLine
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntitySuppliers
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter

open class RotationAnchor(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IParentEntity<IGameEntity>,
    IMotionEntity, IDrawableShapesEntity {

    companion object {
        const val TAG = "RotationAnchor"
        private const val DEFAULT_ROTATION_SPEED = 2f
    }

    override var children = Array<IGameEntity>()

    private val childTargets = ObjectMap<GameEntity, Vector2>()

    override fun getType() = EntityType.SPECIAL

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(MotionComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(DrawableShapesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        clearProdShapeSuppliers()

        val spawn = bounds.getCenter()
        val childEntities = convertObjectPropsToEntitySuppliers(spawnProps)
        childEntities.forEach { (childSupplier, childProps) ->
            val child = childSupplier.invoke()
            if (child !is IBodyEntity) throw IllegalArgumentException("Entity must be an IBodyEntity")

            val childSpawn = childProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
            val length = childSpawn.dst(spawn)
            val speed = childProps.getOrDefault(ConstKeys.SPEED, DEFAULT_ROTATION_SPEED, Float::class)
            val degreesOnReset = calculateAngleDegrees(spawn, childSpawn)
            GameLogger.debug(
                TAG, "spawn(): child: childSpawn=$childSpawn length=$length, speed=$speed, " +
                        "degreesOnReset=$degreesOnReset"
            )
            val rotation = RotatingLine(spawn, length, speed * ConstVals.PPM, degreesOnReset)

            val childKey = childProps.get(ConstKeys.CHILD_KEY)!!
            putMotionDefinition(
                childKey,
                MotionComponent.MotionDefinition(rotation, { target, _ ->
                    // TODO: child.body.setCenter(target)
                    childTargets.put(child, target)
                })
            )
            child.spawn(childProps)
            children.add(child)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        children.forEach { (it as GameEntity).destroy() }
        children.clear()
        clearMotionDefinitions()
    }

    protected open fun defineUpdatablesComponent() = UpdatablesComponent({
        children = children.processAndFilter({ child ->
            child as MegaGameEntity
            if (child.dead) {
                val childKey = child.getProperty(ConstKeys.CHILD_KEY)!!
                removeMotionDefinition(childKey)
            }
        }, { child -> !(child as MegaGameEntity).dead })
    })

    protected open fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.preProcess.put(ConstKeys.DEFAULT) {
            children.forEach { child ->
                child as IBodyEntity
                val target = childTargets[child as GameEntity] ?: return@forEach
                val velocity = target.cpy().sub(child.body.getCenter())
                child.body.physics.velocity.set(velocity.scl(1f / ConstVals.FIXED_TIME_STEP))
            }
        }
        return BodyComponentCreator.create(this, body)
    }

    protected open fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullable))
    }

}
