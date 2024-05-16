package com.megaman.maverick.game.drawables.sprites

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.common.GameLogger
import com.engine.common.interfaces.Updatable
import com.engine.drawables.IDrawable
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpriteMatrix

open class Background(
    startX: Float,
    startY: Float,
    model: TextureRegion,
    modelWidth: Float,
    modelHeight: Float,
    rows: Int,
    columns: Int,
    priority: DrawingPriority = DrawingPriority(DrawingSection.BACKGROUND, 0)
) : Updatable, IDrawable<Batch> {

    companion object {
        const val TAG = "Background"
    }

    protected val backgroundSprites: SpriteMatrix =
        SpriteMatrix(model, priority, modelWidth, modelHeight, rows, columns)

    init {
        backgroundSprites.setPosition(startX, startY)
    }

    override fun update(delta: Float) {
        // optional update method
        GameLogger.debug(TAG, "Updating background: ${(backgroundSprites[0, 0] as GameSprite).boundingRectangle}")
    }

    override fun draw(drawer: Batch) = backgroundSprites.draw(drawer)
}
