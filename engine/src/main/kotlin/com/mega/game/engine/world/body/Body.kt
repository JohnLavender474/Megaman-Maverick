package com.mega.game.engine.world.body

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.exp
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.interfaces.IRectangle
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.IDrawableShape
import kotlin.math.abs

class Body(
    override var type: BodyType,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    override var physics: PhysicsData = PhysicsData(),
    var fixtures: OrderedMap<Any, OrderedSet<IFixture>> = OrderedMap(),
    var preProcess: OrderedMap<Any, () -> Unit> = OrderedMap(),
    var postProcess: OrderedMap<Any, () -> Unit> = OrderedMap(),
    var onReset: OrderedMap<Any, () -> Unit> = OrderedMap(),
    override var direction: Direction = Direction.UP,
    override var properties: Properties = Properties(),
    override var drawingColor: Color = Color.RED,
    override var drawingShapeType: ShapeType = ShapeType.Line
) : IBody, IPropertizable, IDrawableShape {

    companion object {
        const val TAG = "Body"
    }

    private val bounds = GameRectangle(x, y, width, height)
    private val tempRect1 = GameRectangle()
    private val tempVec1 = Vector2()

    override fun getBounds(out: GameRectangle): GameRectangle {
        out.set(bounds)
        val center = bounds.getCenter(tempVec1)
        out.rotate(direction.rotation, center.x, center.y)
        return out
    }

    override fun addFixture(fixture: IFixture): Body {
        val type = fixture.getType()
        val set = fixtures.putIfAbsentAndGet(type, OrderedSet())
        set.add(fixture)
        return this
    }

    override fun hasFixture(fixture: IFixture): Boolean {
        val type = fixture.getType()
        val set = fixtures[type] ?: return false
        return set.contains(type)
    }

    override fun removeFixture(fixture: IFixture): Boolean {
        val type = fixture.getType()
        val set = fixtures[type] ?: return false
        if (!set.contains(fixture)) return false
        set.remove(fixture)
        return true
    }

    override fun getFixtures(out: Array<IFixture>, vararg params: Any): Array<IFixture> {
        when {
            !params.isEmpty() -> params.forEach { type ->
                val set = fixtures[type] ?: return@forEach
                set.forEach { fixture -> out.add(fixture) }
            }

            else -> fixtures.values().forEach { set -> set.forEach { fixture -> out.add(fixture) } }
        }

        return out
    }

    override fun forEachFixture(action: (IFixture) -> Unit): Body {
        fixtures.values().forEach { set -> set.forEach { fixture -> action.invoke(fixture) } }
        return this
    }

    override fun preProcess() = preProcess.values().forEach { it.invoke() }

    override fun process(delta: Float) {
        if (physics.applyFrictionX && physics.frictionOnSelf.x > 0f)
            physics.velocity.x *= exp(-physics.frictionOnSelf.x * delta)
        if (physics.applyFrictionY && physics.frictionOnSelf.y > 0f)
            physics.velocity.y *= exp(-physics.frictionOnSelf.y * delta)
        physics.frictionOnSelf.set(physics.defaultFrictionOnSelf)

        if (physics.gravityOn) physics.velocity.add(physics.gravity)

        physics.velocity.x =
            physics.velocity.x.coerceIn(-abs(physics.velocityClamp.x), abs(physics.velocityClamp.x))
        physics.velocity.y =
            physics.velocity.y.coerceIn(-abs(physics.velocityClamp.y), abs(physics.velocityClamp.y))

        bounds.translate(physics.velocity.x * delta, 0f)
        bounds.translate(0f, physics.velocity.y * delta)
    }

    override fun postProcess() = postProcess.values().forEach { it.invoke() }

    override fun reset() {
        physics.reset()
        onReset.values().forEach { it.invoke() }
    }

    override fun equals(other: Any?) = this === other

    override fun hashCode() = System.identityHashCode(this)

    fun set(bounds: IRectangle): Body {
        setWidth(bounds.getWidth())
        setHeight(bounds.getHeight())
        setX(bounds.getX())
        setY(bounds.getY())
        return this
    }

    fun set(bounds: Rectangle): Body {
        setWidth(bounds.getWidth())
        setHeight(bounds.getHeight())
        setX(bounds.getX())
        setY(bounds.getY())
        return this
    }

    override fun setX(x: Float): Body {
        bounds.setX(x)
        return this
    }

    override fun setY(y: Float): Body {
        bounds.setY(y)
        return this
    }

    override fun setPosition(x: Float, y: Float): Body {
        setX(x)
        setY(y)
        return this
    }

    override fun getX() = bounds.getX()

    override fun getY() = bounds.getY()

    override fun setCenter(x: Float, y: Float): Body {
        bounds.setCenter(x, y)
        return this
    }

    override fun getCenter(out: Vector2) = bounds.getCenter(out)

    override fun getWidth() = bounds.getWidth()

    override fun getHeight() = bounds.getHeight()

    override fun setWidth(width: Float): Body {
        bounds.setWidth(width)
        return this
    }

    override fun setHeight(height: Float): Body {
        bounds.setHeight(height)
        return this
    }

    override fun set(x: Float, y: Float, width: Float, height: Float): Body {
        super.set(x, y, width, height)
        return this
    }

    override fun setPosition(position: Vector2): Body {
        super.setPosition(position)
        return this
    }

    override fun setCenter(center: Vector2): Body {
        super.setCenter(center)
        return this
    }

    override fun setTopLeftToPoint(topLeftPoint: Vector2): Body {
        super.setTopLeftToPoint(topLeftPoint)
        return this
    }

    override fun setTopCenterToPoint(topCenterPoint: Vector2): Body {
        super.setTopCenterToPoint(topCenterPoint)
        return this
    }

    override fun setTopRightToPoint(topRightPoint: Vector2): Body {
        super.setTopRightToPoint(topRightPoint)
        return this
    }

    override fun setCenterLeftToPoint(centerLeftPoint: Vector2): Body {
        super.setCenterLeftToPoint(centerLeftPoint)
        return this
    }

    override fun setCenterToPoint(centerPoint: Vector2): Body {
        super.setCenterToPoint(centerPoint)
        return this
    }

    override fun setCenterRightToPoint(centerRightPoint: Vector2): Body {
        super.setCenterRightToPoint(centerRightPoint)
        return this
    }

    override fun setBottomLeftToPoint(bottomLeftPoint: Vector2): Body {
        super.setBottomLeftToPoint(bottomLeftPoint)
        return this
    }

    override fun setBottomCenterToPoint(bottomCenterPoint: Vector2): Body {
        super.setBottomCenterToPoint(bottomCenterPoint)
        return this
    }

    override fun setBottomRightToPoint(bottomRightPoint: Vector2): Body {
        super.setBottomRightToPoint(bottomRightPoint)
        return this
    }

    override fun setMaxX(x: Float): Body {
        super.setMaxX(x)
        return this
    }

    override fun setMaxY(y: Float): Body {
        super.setMaxY(y)
        return this
    }

    override fun translate(x: Float, y: Float): Body {
        super.translate(x, y)
        return this
    }

    override fun translate(delta: Vector2): Body {
        super.translate(delta)
        return this
    }

    override fun setSize(size: Float): Body {
        super.setSize(size)
        return this
    }

    override fun setSize(width: Float, height: Float): Body {
        super.setSize(width, height)
        return this
    }

    override fun translateSize(width: Float, height: Float): Body {
        super.translateSize(width, height)
        return this
    }

    override fun positionOnPoint(point: Vector2, position: Position): Body {
        super.positionOnPoint(point, position)
        return this
    }

    override fun draw(renderer: ShapeRenderer): Body {
        val bounds = getBounds(tempRect1)
        bounds.drawingColor = drawingColor
        bounds.drawingShapeType = drawingShapeType
        bounds.draw(renderer)
        return this
    }
}
