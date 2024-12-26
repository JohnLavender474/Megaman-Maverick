package com.mega.game.engine.drawables.sprites

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection

class GameSpriteBuilder {

    private var texture: Texture? = null
    private var textureRegion: TextureRegion? = null
    private var x: Float = 0f
    private var y: Float = 0f
    private var width: Float = 0f
    private var height: Float = 0f
    private var priority: DrawingPriority = DrawingPriority(DrawingSection.PLAYGROUND, 0)
    private var hidden: Boolean = false

    fun setTexture(texture: Texture) = apply {
        this.texture = texture
        this.textureRegion = null
    }

    fun setTextureRegion(textureRegion: TextureRegion) = apply {
        this.textureRegion = textureRegion
        this.texture = null
    }

    fun setPosition(x: Float, y: Float) = apply {
        this.x = x
        this.y = y
    }

    fun setSize(width: Float, height: Float) = apply {
        this.width = width
        this.height = height
    }

    fun setPriority(priority: DrawingPriority) = apply {
        this.priority = priority
    }

    fun setHidden(hidden: Boolean) = apply {
        this.hidden = hidden
    }

    fun build() = when {
        texture != null -> GameSprite(texture!!, x, y, width, height, priority, hidden)
        textureRegion != null -> GameSprite(textureRegion!!, x, y, width, height, priority, hidden)
        else -> GameSprite(priority, hidden)
    }
}
