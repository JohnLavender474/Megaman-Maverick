package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.MathUtils
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GamePolygon
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.addBodyLabel
import com.megaman.maverick.game.world.body.getBody
import com.megaman.maverick.game.world.body.getEntity
import kotlin.math.abs
import kotlin.math.min

// TODO: a body that jumps or air dashes up into a slope from below snaps up onto the surface: at the top of the jump
//  (or at once in a dash, with vy = 0) its bottom is inside the triangle, and MegaCollisionHandler pushes it up. A
//  one-way platform lets such a body fall back down instead. Levels do not ask Megaman to pass up through a slope
//  (backlog candidate in docs/mega-refactor/items/068-slopes/branches/mega-refactor-068a-slopes/docs/TODO.md)
class GroundSlope(services: MegaServices) : Block(services) {

    companion object {
        const val TAG = "GroundSlope"

        private const val SHALLOW_RATIO = 0.25f
        private const val STEEP_RATIO = 0.5f

        private const val SHALLOW_STEP_WIDTH = 0.25f
        private const val STEEP_STEP_WIDTH = 0.125f
        private const val RATIO_TOLERANCE = 0.01f
        private const val STEP_TOLERANCE = 0.01f * ConstVals.PPM

        private val FILL_COLOR = Color(0.45f, 0.3f, 0.6f, 1f)
    }

    var highOnRight = true
        private set

    private var ratio = SHALLOW_RATIO
    private var stepWidth = SHALLOW_STEP_WIDTH * ConstVals.PPM

    private val triangle = GamePolygon()
    private val vertices = FloatArray(6)

    private val fill = object : IDrawableShape {
        override var drawingColor = FILL_COLOR
        override var drawingShapeType = ShapeType.Filled

        override fun draw(renderer: ShapeRenderer): IDrawableShape {
            renderer.color = drawingColor
            renderer.set(drawingShapeType)
            val topX = if (highOnRight) body.getMaxX() else body.getX()
            renderer.triangle(body.getX(), body.getY(), body.getMaxX(), body.getY(), topX, body.getMaxY())
            return this
        }
    }

    override fun init(vararg params: Any) {
        super.init()
        getComponent(DrawableShapesComponent::class)!!.prodShapeSuppliers.add { fill }
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        blockFixture.setShape(triangle)
        component.body.preProcess.put("${ConstKeys.BLOCK}_${ConstKeys.FIXTURE}") {
            blockFixture.drawingColor = if (blockFixture.isActive()) Color.RED else Color.GRAY
        }
        return component
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val side = spawnProps.get(ConstKeys.SIDE, String::class)?.lowercase()
        highOnRight = when (side) {
            ConstKeys.RIGHT -> true
            ConstKeys.LEFT -> false
            else -> {
                GameLogger.error(TAG, "onSpawn(): side must be left or right, got $side; using right")
                true
            }
        }

        val width = body.getWidth()
        val height = body.getHeight()
        val actualRatio = if (width > 0f) height / width else 0f
        ratio = when {
            abs(actualRatio - SHALLOW_RATIO) <= RATIO_TOLERANCE -> SHALLOW_RATIO
            abs(actualRatio - STEEP_RATIO) <= RATIO_TOLERANCE -> STEEP_RATIO
            else -> {
                val nearer =
                    if (abs(actualRatio - SHALLOW_RATIO) <= abs(actualRatio - STEEP_RATIO)) SHALLOW_RATIO
                    else STEEP_RATIO
                GameLogger.error(
                    TAG, "onSpawn(): height/width must be 1/4 or 1/2, got $actualRatio ($width x $height); using $nearer"
                )
                nearer
            }
        }
        stepWidth = (if (ratio == SHALLOW_RATIO) SHALLOW_STEP_WIDTH else STEEP_STEP_WIDTH) * ConstVals.PPM

        vertices[0] = 0f
        vertices[1] = 0f
        vertices[2] = width
        vertices[3] = 0f
        if (highOnRight) {
            vertices[4] = width
            vertices[5] = height
        } else {
            vertices[4] = 0f
            vertices[5] = height
        }
        triangle.setLocalVertices(vertices)

        body.addBodyLabel(BodyLabel.SLOPE)
    }

    override fun hitByFeet(processState: ProcessState, feetFixture: IFixture) {
        if (processState != ProcessState.CONTINUE) return

        // TODO: a body that climbs down a ladder whose bottom is on a slope snaps onto the surface; no ladder ends on
        //  a slope today (see docs/mega-refactor/items/068-slopes/branches/mega-refactor-068a-slopes/docs/NOTES.md)
        val feetBody = feetFixture.getBody()
        if (!feetBody.physics.gravityOn || feetBody.physics.velocity.y > 0f) return

        val entity = feetBody.getEntity()
        if (entity is IDirectional && entity.direction != Direction.UP) return

        val x = feetBody.getX() + feetBody.getWidth() / 2f
        val pastLowEnd = if (highOnRight) x < body.getX() else x > body.getMaxX()
        val target = when {
            x >= body.getX() && x <= body.getMaxX() -> surfaceAt(x)
            pastLowEnd && feetBody.getY() <= body.getY() + stepWidth * ratio + STEP_TOLERANCE -> body.getY()
            else -> return
        }

        if (feetBody.getY() > target) feetBody.setY(target)
    }

    fun surfaceAt(x: Float): Float {
        val width = body.getWidth()
        val fromLowEnd = if (highOnRight) x - body.getX() else body.getMaxX() - x
        val distance = fromLowEnd.coerceIn(0f, width)
        val step = MathUtils.floor(distance / stepWidth)
        return body.getY() + min(body.getHeight(), (step + 1) * stepWidth * ratio)
    }

    override fun getTag() = TAG
}
