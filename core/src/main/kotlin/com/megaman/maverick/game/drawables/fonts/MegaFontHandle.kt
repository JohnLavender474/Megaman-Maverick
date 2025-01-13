package com.megaman.maverick.game.drawables.fonts

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
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

    companion object {
        const val TAG = "MegaFontHandle"
        private var font: BitmapFont? = null
    }

    override val priority: DrawingPriority
        get() = fontHandle.priority
    override var drawingColor: Color = Color.RED
    override var drawingShapeType: ShapeType = ShapeType.Line

    var positionX: Float
        get() = fontHandle.getX()
        set(value) = fontHandle.setX(value)
    var positionY: Float
        get() = fontHandle.getY()
        set(value) = fontHandle.setY(value)

    private val fontHandle: BitmapFontHandle

    constructor(
        text: String,
        positionX: Float,
        positionY: Float,
        attachment: Position,
        hidden: Boolean = false,
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 0)
    ) {
        if (font == null) font = BitmapFontHandle.loadFont(ConstVals.MEGAMAN_MAVERICK_FONT, getDefaultFontSize())
        fontHandle = BitmapFontHandle(
            textSupplier = { text },
            font = font!!,
            positionX = positionX,
            positionY = positionY,
            attachment = attachment,
            priority = priority,
            hidden = hidden,
        )
    }

    constructor(
        textSupplier: () -> String,
        positionX: Float = (ConstVals.VIEW_WIDTH - 5) * ConstVals.PPM,
        positionY: Float = (ConstVals.VIEW_HEIGHT - 1) * ConstVals.PPM,
        centerX: Boolean = true,
        centerY: Boolean = true,
        hidden: Boolean = false,
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 10)
    ) {
        if (font == null) font = BitmapFontHandle.loadFont(ConstVals.MEGAMAN_MAVERICK_FONT, getDefaultFontSize())
        val attachment = BitmapFontHandle.getAttachment(centerX, centerY)
        fontHandle = BitmapFontHandle(
            textSupplier = textSupplier,
            font = font!!,
            positionX = positionX,
            positionY = positionY,
            attachment = attachment,
            hidden = hidden,
            priority = priority
        )
    }

    constructor(
        text: String,
        positionX: Float = (ConstVals.VIEW_WIDTH - 5) * ConstVals.PPM,
        positionY: Float = (ConstVals.VIEW_HEIGHT - 1) * ConstVals.PPM,
        centerX: Boolean = true,
        centerY: Boolean = true,
        hidden: Boolean = false,
        priority: DrawingPriority = DrawingPriority(DrawingSection.FOREGROUND, 10)
    ) : this(
        { text },
        positionX = positionX,
        positionY = positionY,
        centerX = centerX,
        centerY = centerY,
        hidden = hidden,
        priority = priority
    )

    override fun draw(drawer: Batch) = fontHandle.draw(drawer)

    override fun draw(renderer: ShapeRenderer): IDrawableShape {
        fontHandle.draw(renderer)
        return this
    }

    fun getCurrentText() = fontHandle.textSupplier()

    fun setText(text: String) {
        fontHandle.textSupplier = { text }
    }

    fun setTextSupplier(supplier: () -> String) {
        fontHandle.textSupplier = supplier
    }

    override fun getX() = fontHandle.getX()

    override fun getY() = fontHandle.getY()

    override fun setX(x: Float) = fontHandle.setX(x)

    override fun setY(y: Float) = fontHandle.setY(y)

    fun getFontWidth() = fontHandle.getFontWidth()

    fun getFontHeight() = fontHandle.getFontHeight()
}