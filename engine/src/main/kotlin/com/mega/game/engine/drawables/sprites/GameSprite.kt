package com.mega.game.engine.drawables.sprites

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sorting.IComparableDrawable

class GameSprite(
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

    override fun draw(drawer: Batch) {
        if (!hidden && texture != null) super.draw(drawer)
    }

    override fun compareTo(other: IComparableDrawable<Batch>) = priority.compareTo(other.priority)
}
