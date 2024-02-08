package com.megaman.maverick.game.drawables.sprites

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.common.interfaces.Updatable
import com.engine.drawables.IDrawable
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sprites.SpriteMatrix

open class Background(
    startX: Float,
    startY: Float,
    model: TextureRegion,
    priority: DrawingPriority,
    modelWidth: Float,
    modelHeight: Float,
    rows: Int,
    columns: Int
) : Updatable, IDrawable<Batch> {

    protected val backgroundSprites: SpriteMatrix =
        SpriteMatrix(model, priority, modelWidth, modelHeight, rows, columns)

    init {
        backgroundSprites.setPosition(startX, startY)
    }

    override fun update(delta: Float) {
        // optional update method
    }

    override fun draw(drawer: Batch) = backgroundSprites.draw(drawer)
}
