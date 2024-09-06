package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.calculateAngleDegrees
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.processAndFilter
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.GameEntity
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
import com.megaman.maverick.game.world.body.BodyComponentCreator

open class RotationAnchor(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IParentEntity, IMotionEntity,
    IDrawableShapesEntity {

    companion object {
        const val TAG = "RotationAnchor"
        private const val DEFAULT_ROTATION_SPEED = 2f
    }

    override var children = Array<GameEntity>()

    private val childTargets = ObjectMap<GameEntity, Vector2>()

    override fun getEntityType() = EntityType.SPECIAL

    override fun init() {
        addComponent(MotionComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(DrawableShapesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
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
            if (childProps.get(ConstKeys.DRAW_LINE) == true) {
                val color = childProps.get(ConstKeys.COLOR, String::class)!!
                rotation.line.color = Color.valueOf(color)
                addProdShapeSupplier { rotation.line }
            }

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
        children.forEach { it.destroy() }
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
        body.preProcess.put(ConstKeys.DEFAULT) { delta ->
            children.forEach { child ->
                child as IBodyEntity
                val target = childTargets[child] ?: return@forEach
                val velocity = target.cpy().sub(child.body.getCenter())
                child.body.physics.velocity.set(velocity.scl(1f / delta))
            }
        }
        return BodyComponentCreator.create(this, body)
    }

    protected open fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS to cullable))
    }

}