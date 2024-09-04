package com.megaman.maverick.game.drawables.sprites

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpriteMatrix

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
