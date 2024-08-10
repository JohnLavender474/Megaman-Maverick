package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.GameLogger
import com.engine.common.calculateAngleDegrees
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.processAndFilter
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IDrawableShapesEntity
import com.engine.entities.contracts.IMotionEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.motion.MotionComponent
import com.engine.motion.RotatingLine
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntitySuppliers
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.BodyComponentCreator

open class RotationAnchor(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IParentEntity, IMotionEntity,
    IDrawableShapesEntity {

    companion object {
        const val TAG = "RotationAnchor"
        private const val DEFAULT_ROTATION_SPEED = 2f
    }

    override var children = Array<IGameEntity>()

    private val childTargets = ObjectMap<IGameEntity, Vector2>()

    override fun init() {
        addComponent(MotionComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(DrawableShapesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

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

            game.engine.spawn(child, childProps)
            children.add(child)
        }
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
        children.forEach { it.kill() }
        children.clear()
        clearMotionDefinitions()
    }

    protected open fun defineUpdatablesComponent() = UpdatablesComponent({
        children = children.processAndFilter({ child ->
            if (child.dead) {
                val childKey = child.getProperty(ConstKeys.CHILD_KEY)!!
                removeMotionDefinition(childKey)
            }
        }, { child -> !child.dead })
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