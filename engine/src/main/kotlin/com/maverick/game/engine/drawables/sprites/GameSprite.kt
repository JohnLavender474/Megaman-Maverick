package com.maverick.game.engine.drawables.sprites

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sorting.IComparableDrawable

open class GameSprite(
    override val priority: DrawingPriority = DrawingPriority(DrawingSection.PLAYGROUND, 0),
    var hidden: Boolean = false
) : Sprite(), IComparableDrawable<Batch> {

    constructor(
        texture: Texture,
        priority: DrawingPriority = DrawingPriority(DrawingSection.PLAYGROUND, 0),
        hidden: Boolean = false
    ) : this(priority, hidden) {
        setTexture(texture)
    }

    constructor(
        textureRegion: TextureRegion,
        priority: DrawingPriority = DrawingPriority(DrawingSection.PLAYGROUND, 0),
        hidden: Boolean = false
    ) : this(priority, hidden) {
        setRegion(textureRegion)
    }

    constructor(
        textureRegion: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        priority: DrawingPriority = DrawingPriority(DrawingSection.PLAYGROUND, 0),
        hidden: Boolean = false
    ) : this(priority, hidden) {
        setRegion(textureRegion)
        setPosition(x, y)
        setSize(width, height)
    }

    constructor(
        texture: Texture,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        priority: DrawingPriority = DrawingPriority(DrawingSection.PLAYGROUND, 0),
        hidden: Boolean = false
    ) : this(priority, hidden) {
        setTexture(texture)
        setPosition(x, y)
        setSize(width, height)
    }

    companion object {
        private const val X1 = 0
        private const val Y1 = 1
        private const val VERTEX_STRIDE = 5
        private const val VERTICES_SIZE = 4 * VERTEX_STRIDE
        private val shiftedVertices = FloatArray(VERTICES_SIZE)
    }

    var drawOffsetX = 0f
    var drawOffsetY = 0f

    override fun draw(drawer: Batch) {
        if (hidden || texture == null) return
        if (drawOffsetX == 0f && drawOffsetY == 0f) {
            super.draw(drawer)
            return
        }
        getVertices().copyInto(shiftedVertices)
        for (i in 0 until VERTICES_SIZE step VERTEX_STRIDE) {
            shiftedVertices[i + X1] += drawOffsetX
            shiftedVertices[i + Y1] += drawOffsetY
        }
        drawer.draw(texture, shiftedVertices, 0, VERTICES_SIZE)
    }

    override fun compareTo(other: IComparableDrawable<Batch>) = priority.compareTo(other.priority)
}
