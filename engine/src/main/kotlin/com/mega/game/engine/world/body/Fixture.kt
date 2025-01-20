package com.mega.game.engine.world.body

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.interfaces.ICopyable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.shapes.IRotatableShape
import com.mega.game.engine.drawables.shapes.IDrawableShape

class Fixture(
    var body: Body,
    type: Any,
    rawShape: IGameShape2D = GameRectangle(),
    active: Boolean = true,
    var attachedToBody: Boolean = true,
    var bodyAttachmentPosition: Position = Position.CENTER,
    var offsetFromBodyAttachment: Vector2 = Vector2(),
    override var properties: Properties = Properties(),
    override var drawingColor: Color = Color.RED,
    override var drawingShapeType: ShapeType = ShapeType.Line
) : IFixture, ICopyable<Fixture>, IDrawableShape {

    companion object {
        const val TAG = "Fixture"
    }

    var rawShape: IGameShape2D = rawShape
        set(value) {
            field = value
            adjustedShape = null
        }

    private val fixtureType = type

    private var adjustedShape: IGameShape2D? = null
    private var isActive = active

    private val reusableShapeProps = Properties()
    private val reusableGameRect = GameRectangle()
    private val out1 = Vector2()

    override fun setShape(shape: IGameShape2D) {
        rawShape = shape
    }

    override fun getShape(): IGameShape2D {
        if (adjustedShape == null) adjustedShape = rawShape.copy()

        val copy = adjustedShape!!

        reusableShapeProps.clear()
        rawShape.getProps(reusableShapeProps)
        copy.setWithProps(reusableShapeProps)

        if (!attachedToBody) return adjustedShape!!

        val attachmentPos = body.getBounds(reusableGameRect).getPositionPoint(bodyAttachmentPosition, out1)
        copy.setCenter(attachmentPos).translate(offsetFromBodyAttachment)

        if (copy is IRotatableShape) copy.rotate(body.direction.rotation, attachmentPos.x, attachmentPos.y)

        return copy
    }

    override fun getType() = fixtureType

    override fun setActive(active: Boolean) {
        isActive = active
    }

    override fun isActive() = isActive

    override fun toString() = "Fixture(type=$fixtureType, active=$isActive, shape=${getShape()}, raw_shape=$rawShape)"

    fun overlaps(other: IGameShape2D) = getShape().overlaps(other)

    fun overlaps(other: IFixture) = overlaps(other.getShape())

    override fun copy() = Fixture(
        body,
        getType(),
        rawShape.copy(),
        isActive,
        attachedToBody,
        bodyAttachmentPosition,
        offsetFromBodyAttachment.cpy(),
        Properties(properties)
    )

    override fun draw(renderer: ShapeRenderer): Fixture {
        val shape = getShape()
        shape.drawingColor = drawingColor
        shape.drawingShapeType = drawingShapeType
        shape.draw(renderer)
        return this
    }
}
