package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Array
import com.engine.common.enums.Direction
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import kotlin.math.roundToInt

class Electrocutie(game: MegamanMaverickGame) : GameEntity(game), IHazard, IBodyEntity, IParentEntity {

    companion object {
        const val TAG = "ElectrocutieParent"
        const val SPEED = 2f
    }

    override val children = Array<IGameEntity>()

    private var vertical = true
    private var left = true
    private var minPosition = 0f
    private var maxPosition = 0f

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        if (!children.isEmpty) throw IllegalStateException("Children array should be empty when spawning ElectrocutieParent")

        left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        val min = spawnProps.get(ConstKeys.MIN, Float::class)!!
        val max = spawnProps.get(ConstKeys.MAX, Float::class)!!

        vertical = spawnProps.getOrDefault(ConstKeys.VERTICAL, true, Boolean::class)
        if (vertical) {
            minPosition = bounds.getCenter().x - (min * ConstVals.PPM)
            maxPosition = bounds.getCenter().x + (max * ConstVals.PPM)

            // bottom electrocutie child
            val bottomElectrocutieChildProps = props(
                ConstKeys.POSITION to bounds.getBottomCenterPoint(), ConstKeys.DIRECTION to Direction.UP
            )
            val bottomElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            game.gameEngine.spawn(bottomElectrocutieChild, bottomElectrocutieChildProps)
            children.add(bottomElectrocutieChild)

            // top electrocutie child
            val topElectrocutieProps = props(
                ConstKeys.POSITION to bounds.getTopCenterPoint(), ConstKeys.DIRECTION to Direction.DOWN
            )
            val topElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            game.gameEngine.spawn(topElectrocutieChild, topElectrocutieProps)
            children.add(topElectrocutieChild)

            // bolts
            val length = bounds.height.roundToInt() / ConstVals.PPM
            for (i in 0 until length) {
                val position = body.getBottomCenterPoint().add(0f, i * ConstVals.PPM.toFloat())
                val bolt = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.BOLT)!! as Bolt

                game.gameEngine.spawn(
                    bolt, props(
                        ConstKeys.POSITION to position, ConstKeys.VERTICAL to true
                    )
                )

                children.add(bolt)
            }
        } else {
            minPosition = bounds.getCenter().y - (min * ConstVals.PPM)
            maxPosition = bounds.getCenter().y + (max * ConstVals.PPM)

            // bottom electrocutie child
            val bottomElectrocutieProps = props(
                ConstKeys.POSITION to bounds.getCenterLeftPoint(), ConstKeys.DIRECTION to Direction.RIGHT
            )
            val bottomElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            game.gameEngine.spawn(bottomElectrocutieChild, bottomElectrocutieProps)
            children.add(bottomElectrocutieChild)

            // top electrocutie child
            val topElectrocutieProps = props(
                ConstKeys.POSITION to bounds.getCenterRightPoint(), ConstKeys.DIRECTION to Direction.LEFT
            )
            val topElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            game.gameEngine.spawn(topElectrocutieChild, topElectrocutieProps)
            children.add(topElectrocutieChild)

            // bolts
            val length = bounds.width.roundToInt() / ConstVals.PPM
            for (i in 0 until length) {
                val position = body.getCenterLeftPoint().add(i * ConstVals.PPM.toFloat(), 0f)
                val bolt = Bolt(getMegamanMaverickGame())

                game.gameEngine.spawn(
                    bolt, props(
                        ConstKeys.POSITION to position, ConstKeys.VERTICAL to false
                    )
                )

                children.add(bolt)
            }
        }
    }

    override fun onDestroy() {
        super<GameEntity>.onDestroy()
        children.forEach { it.kill() }
        children.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, {
        val currentPosition = if (vertical) body.getCenter().x else body.getCenter().y

        if (left && currentPosition <= minPosition) left = false
        else if (!left && currentPosition >= maxPosition) left = true

        body.physics.velocity.x = (if (left) -SPEED else SPEED) * ConstVals.PPM
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        body.preProcess.put(ConstKeys.DEFAULT) {
            children.forEach {
                if (it is IBodyEntity) it.body.setCenterX(body.getCenter().x)
            }
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

}