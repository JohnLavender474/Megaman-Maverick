package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.math.roundToInt

class Electrocutie(game: MegamanMaverickGame) : MegaGameEntity(game), IHazard, IBodyEntity, IParentEntity {

    enum class ElectrocutieState { MOVE, CHARGE, SHOCK }

    companion object {
        const val TAG = "ElectrocutieParent"
        const val SPEED = 2f
        const val MOVE_DURATION = 1f
        const val CHARGE_DURATION = 0.75f
        const val SHOCK_DURATION = 0.5f
    }

    override var children = Array<IGameEntity>()

    val currentState: ElectrocutieState
        get() = loop.getCurrent()

    private val loop = Loop(ElectrocutieState.values().toGdxArray())
    private val timers = objectMapOf(
        ElectrocutieState.MOVE pairTo Timer(MOVE_DURATION),
        ElectrocutieState.CHARGE pairTo Timer(CHARGE_DURATION),
        ElectrocutieState.SHOCK pairTo Timer(SHOCK_DURATION)
    )
    private var vertical = true
    private var left = true
    private var minPosition = 0f
    private var maxPosition = 0f

    override fun getEntityType() = EntityType.HAZARD

    override fun getTag() = TAG

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        if (!children.isEmpty) throw IllegalStateException("Children array should be empty when spawning ElectrocutieParent")
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
        super.onSpawn(spawnProps)

        loop.reset()
        timers.values().forEach { it.reset() }

        left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        val min = spawnProps.get(ConstKeys.MIN, Float::class)!!
        val max = spawnProps.get(ConstKeys.MAX, Float::class)!!

        vertical = spawnProps.getOrDefault(ConstKeys.VERTICAL, true, Boolean::class)
        if (vertical) {
            minPosition = bounds.getCenter().x - (min * ConstVals.PPM)
            maxPosition = bounds.getCenter().x + (max * ConstVals.PPM)

            val bottomElectrocutieChildProps = props(
                ConstKeys.POSITION pairTo bounds.getPositionPoint(Position.BOTTOM_CENTER),
                ConstKeys.DIRECTION pairTo Direction.UP,
                ConstKeys.PARENT pairTo this
            )
            val bottomElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            bottomElectrocutieChild.spawn(bottomElectrocutieChildProps)
            children.add(bottomElectrocutieChild)

            val topElectrocutieProps = props(
                ConstKeys.POSITION pairTo bounds.getPositionPoint(Position.TOP_CENTER),
                ConstKeys.DIRECTION pairTo Direction.DOWN,
                ConstKeys.PARENT pairTo this
            )
            val topElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            topElectrocutieChild.spawn(topElectrocutieProps)
            children.add(topElectrocutieChild)

            val length = bounds.getHeight().roundToInt() / ConstVals.PPM
            for (i in 0 until length) {
                val position = body.getPositionPoint(Position.BOTTOM_CENTER).add(0f, i * ConstVals.PPM.toFloat())
                val bolt = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.BOLT)!! as Bolt
                bolt.spawn(
                    props(
                        ConstKeys.POSITION pairTo position,
                        ConstKeys.DIRECTION pairTo Direction.UP,
                        ConstKeys.PARENT pairTo this,
                        ConstKeys.CULL_OUT_OF_BOUNDS pairTo false
                    )
                )
                children.add(bolt)
            }
        } else {
            minPosition = bounds.getCenter().y - (min * ConstVals.PPM)
            maxPosition = bounds.getCenter().y + (max * ConstVals.PPM)

            val bottomElectrocutieProps = props(
                ConstKeys.POSITION pairTo bounds.getPositionPoint(Position.CENTER_LEFT),
                ConstKeys.DIRECTION pairTo Direction.RIGHT,
                ConstKeys.PARENT pairTo this
            )
            val bottomElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            bottomElectrocutieChild.spawn(bottomElectrocutieProps)
            children.add(bottomElectrocutieChild)

            val topElectrocutieProps = props(
                ConstKeys.POSITION pairTo bounds.getPositionPoint(Position.CENTER_RIGHT),
                ConstKeys.DIRECTION pairTo Direction.LEFT,
                ConstKeys.PARENT pairTo this
            )
            val topElectrocutieChild =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ELECTROCUTIE_CHILD)!! as ElectrocutieChild
            topElectrocutieChild.spawn(topElectrocutieProps)
            children.add(topElectrocutieChild)

            val length = bounds.getWidth().roundToInt() / ConstVals.PPM
            for (i in 0 until length) {
                val position = body.getPositionPoint(Position.CENTER_LEFT).add(i * ConstVals.PPM.toFloat(), 0f)
                val bolt = Bolt(game)
                bolt.spawn(
                    props(
                        ConstKeys.POSITION pairTo position,
                        ConstKeys.DIRECTION pairTo Direction.RIGHT,
                        ConstKeys.PARENT pairTo this,
                        ConstKeys.CULL_OUT_OF_BOUNDS pairTo false
                    )
                )
                children.add(bolt)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        children.forEach { (it as GameEntity).destroy() }
        children.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val currentPosition = if (vertical) body.getCenter().x else body.getCenter().y

        if (left && currentPosition <= minPosition) left = false
        else if (!left && currentPosition >= maxPosition) left = true

        val speed = (if (left) -SPEED else SPEED) * ConstVals.PPM
        body.physics.velocity = if (vertical) Vector2(speed, 0f)
        else Vector2(0f, speed)

        val currentState = loop.getCurrent()

        val timer = timers.get(currentState)
        timer.update(delta)
        if (timer.isFinished()) {
            timer.reset()
            loop.next()
        }

        val shock = currentState == ElectrocutieState.SHOCK
        children.forEach { child ->
            if (child is Bolt) {
                child.body.forEachFixture { it.setActive(shock) }
                child.sprites.values().forEach { childSprite -> childSprite.hidden = !shock }
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        body.preProcess.put(ConstKeys.DEFAULT) {
            children.forEach {
                if (it is IBodyEntity) {
                    if (vertical) it.body.setCenterX(body.getCenter().x) else it.body.setCenterY(body.getCenter().y)
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

}
