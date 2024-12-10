package com.megaman.maverick.game.drawables.fonts

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.interfaces.IPositional
import com.mega.game.engine.drawables.fonts.BitmapFontHandle
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sorting.IComparableDrawable
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize

class MegaFontHandle : IComparableDrawable<Batch>, IPositional, IDrawableShape {

    override val priority: DrawingPriority
        get() = font.priority
    override var drawingColor: Color = Color.RED
    override var drawingShapeType: ShapeType = ShapeType.Line

    var positionX: Float
        get() = font.getX()
        set(value) = font.setX(value)
    var positionY: Float
        get() = font.getY()
        set(value) = font.setY(value)

    private val font: BitmapFontHandle

    constructor(
        textSupplier: () -> String,
        positionX: Float,
        positionY: Float,
        attachment: Position,
        hidden: Boolean = false,
        fontSource: String = ConstVals.MEGAMAN_MAVERICK_FONT,
        fontSize: Int = getDefaultFontSize(),
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 0),
    ) {
        font = BitmapFontHandle(
            textSupplier,
            positionX,
            positionY,
            attachment,
            hidden,
            fontSource,
            fontSize,
            priority
        )
    }

    constructor(
        text: String,
        positionX: Float,
        positionY: Float,
        attachment: Position,
        hidden: Boolean = false,
        fontSource: String = ConstVals.MEGAMAN_MAVERICK_FONT,
        fontSize: Int = getDefaultFontSize(),
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 0)
    ) {
        font = BitmapFontHandle(
            text,
            positionX,
            positionY,
            attachment,
            hidden,
            fontSource,
            fontSize,
            priority
        )
    }

    constructor(
        textSupplier: () -> String,
        fontSize: Int = getDefaultFontSize(),
        positionX: Float = (ConstVals.VIEW_WIDTH - 5) * ConstVals.PPM,
        positionY: Float = (ConstVals.VIEW_HEIGHT - 1) * ConstVals.PPM,
        centerX: Boolean = true,
        centerY: Boolean = true,
        hidden: Boolean = false,
        fontSource: String = ConstVals.MEGAMAN_MAVERICK_FONT,
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 10)
    ) {
        font = BitmapFontHandle(
            textSupplier,
            fontSize,
            Vector2(positionX, positionY),
            centerX,
            centerY,
            hidden,
            fontSource,
            priority
        )
    }

    constructor(
        text: String,
        fontSize: Int = getDefaultFontSize(),
        positionX: Float = (ConstVals.VIEW_WIDTH - 5) * ConstVals.PPM,
        positionY: Float = (ConstVals.VIEW_HEIGHT - 1) * ConstVals.PPM,
        centerX: Boolean = true,
        centerY: Boolean = true,
        hidden: Boolean = false,
        fontSource: String = ConstVals.MEGAMAN_MAVERICK_FONT,
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 10)
    ) : this(
        { text },
        fontSize = fontSize,
        positionX = positionX,
        positionY = positionY,
        centerX = centerX,
        centerY = centerY,
        hidden = hidden,
        fontSource = fontSource,
        priority = priority
    )

    override fun draw(drawer: Batch) = font.draw(drawer)

    override fun draw(renderer: ShapeRenderer): IDrawableShape {
        font.draw(renderer)
        return this
    }

    fun getCurrentText() = font.textSupplier()

    fun setText(text: String) {
        font.textSupplier = { text }
    }

    fun setTextSupplier(supplier: () -> String) {
        font.textSupplier = supplier
    }

    override fun getX() = font.getX()

    override fun getY() = font.getY()

    override fun setX(x: Float) = font.setX(x)

    override fun setY(y: Float) = font.setY(y)

    fun getFontWidth() = font.getFontWidth()

    fun getFontHeight() = font.getFontHeight()
}
