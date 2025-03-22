package com.mega.game.engine.drawables.fonts

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.interfaces.IPositional
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sorting.IComparableDrawable

class BitmapFontHandle(
    var textSupplier: () -> String,
    var positionX: Float,
    var positionY: Float,
    var attachment: Position,
    val font: BitmapFont = BitmapFont(),
    var hidden: Boolean = false,
    var alpha: Float = 1f,
    override val priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 0),
    override var drawingColor: Color = Color.WHITE,
    override var drawingShapeType: ShapeType = ShapeType.Line
) : IComparableDrawable<Batch>, IPositional, IDrawableShape {

    companion object {
        const val TAG = "BitmapFontHandle"
        const val DEFAULT_FONT_SIZE = 10

        fun loadFont(fontSource: String, fontSize: Int): BitmapFont {
            val generator = FreeTypeFontGenerator(Gdx.files.internal(fontSource))

            val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
            parameter.size = fontSize

            val font = generator.generateFont(parameter)
            generator.dispose()

            return font
        }

        fun getAttachment(centerX: Boolean, centerY: Boolean) = when {
            centerX && centerY -> Position.CENTER
            centerX -> Position.BOTTOM_CENTER
            centerY -> Position.CENTER_LEFT
            else -> Position.BOTTOM_LEFT
        }
    }

    private val layout = GlyphLayout()
    private val out = Vector2()

    constructor(
        text: String,
        positionX: Float,
        positionY: Float,
        attachment: Position,
        fontSource: String,
        hidden: Boolean = false,
        fontSize: Int = DEFAULT_FONT_SIZE,
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 0),
        drawingColor: Color = Color.RED,
        drawingShapeType: ShapeType = ShapeType.Line
    ) : this(
        textSupplier = { text },
        font = loadFont(fontSource, fontSize),
        positionX = positionX,
        positionY = positionY,
        attachment = attachment,
        hidden = hidden,
        priority = priority,
        drawingColor = drawingColor,
        drawingShapeType = drawingShapeType
    )

    constructor(
        textSupplier: () -> String,
        positionX: Float,
        positionY: Float,
        attachment: Position,
        fontSource: String,
        hidden: Boolean = false,
        fontSize: Int = DEFAULT_FONT_SIZE,
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 0),
        drawingColor: Color = Color.RED,
        drawingShapeType: ShapeType = ShapeType.Line
    ) : this(
        textSupplier = textSupplier,
        font = loadFont(fontSource, fontSize),
        positionX = positionX,
        positionY = positionY,
        attachment = attachment,
        hidden = hidden,
        priority = priority,
        drawingColor = drawingColor,
        drawingShapeType = drawingShapeType
    )

    constructor(
        text: String,
        fontSource: String,
        positionX: Float,
        positionY: Float,
        attachment: Position,
        hidden: Boolean = false,
        fontSize: Int = DEFAULT_FONT_SIZE,
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 0),
    ) : this(
        textSupplier = { text },
        positionX = positionX,
        positionY = positionY,
        attachment = attachment,
        hidden = hidden,
        fontSource = fontSource,
        fontSize = fontSize,
        priority = priority
    )

    // keep this to retain compatibility with code that uses the old primary constructor
    constructor(
        textSupplier: () -> String,
        fontSource: String,
        fontSize: Int = DEFAULT_FONT_SIZE,
        position: Vector2 = Vector2(),
        centerX: Boolean = true,
        centerY: Boolean = true,
        hidden: Boolean = false,
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 0)
    ) : this(
        textSupplier = textSupplier,
        positionX = position.x,
        positionY = position.y,
        attachment = getAttachment(centerX, centerY),
        hidden = hidden,
        fontSource = fontSource,
        fontSize = fontSize,
        priority = priority
    )

    // keep this to retain compatibility with code that uses the old primary constructor
    constructor(
        text: String,
        fontSource: String,
        fontSize: Int = DEFAULT_FONT_SIZE,
        position: Vector2 = Vector2(),
        centerX: Boolean = true,
        centerY: Boolean = true,
        hidden: Boolean = false,
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 0)
    ) : this(
        text = text,
        positionX = position.x,
        positionY = position.y,
        attachment = getAttachment(centerX, centerY),
        hidden = hidden,
        fontSource = fontSource,
        fontSize = fontSize,
        priority = priority
    )

    fun getFontWidth() = layout.width

    fun getFontHeight() = layout.height

    private fun getFontPosition(out: Vector2) = when (attachment) {
        Position.BOTTOM_LEFT -> out.set(positionX, positionY)
        Position.BOTTOM_CENTER -> out.set(positionX - getFontWidth() / 2f, positionY)
        Position.BOTTOM_RIGHT -> out.set(positionX - getFontWidth(), positionY)
        Position.CENTER_LEFT -> out.set(positionX, positionY - getFontHeight() / 2f)
        Position.CENTER -> out.set(positionX - getFontWidth() / 2f, positionY - getFontHeight() / 2f)
        Position.CENTER_RIGHT -> out.set(positionX - getFontWidth(), positionY - getFontHeight() / 2f)
        Position.TOP_LEFT -> out.set(positionX, positionY - getFontHeight())
        Position.TOP_CENTER -> out.set(positionX - getFontWidth() / 2f, positionY - getFontHeight())
        Position.TOP_RIGHT -> out.set(positionX - getFontWidth(), positionY - getFontHeight())
    }

    override fun draw(renderer: ShapeRenderer): IDrawableShape {
        val position = getFontPosition(out)
        renderer.color = drawingColor
        renderer.set(drawingShapeType)
        renderer.rect(position.x, position.y, getFontWidth(), getFontHeight())
        return this
    }

    override fun draw(drawer: Batch) {
        if (hidden) return
        layout.setText(font, textSupplier())
        val position = getFontPosition(out)
        font.setColor(drawingColor.r, drawingColor.g, drawingColor.b, alpha)
        font.draw(drawer, layout, position.x, position.y)
    }

    override fun setX(x: Float) {
        positionX = x
    }

    override fun setY(y: Float) {
        positionY = y
    }

    override fun getX() = positionX

    override fun getY() = positionY
}
