package com.megaman.maverick.game.drawables.sprites

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection

open class AnimatedBackground(
    startX: Float,
    startY: Float,
    model: TextureRegion,
    modelWidth: Float,
    modelHeight: Float,
    rows: Int,
    columns: Int,
    animRows: Int,
    animColumns: Int,
    duration: Float,
    priority: DrawingPriority = DrawingPriority(DrawingSection.BACKGROUND, 0)
) : Background(startX, startY, model, modelWidth, modelHeight, rows, columns, priority) {

    companion object {
        const val TAG = "AnimatedBackground"
    }

    protected val animations: Matrix<Animation> =
        Matrix(rows, columns) { _, _ -> Animation(model, animRows, animColumns, duration, true) }

    override fun update(delta: Float) {
        animations.forEach { x, y, it ->
            val sprite = backgroundSprites[x, y]
            if (sprite == null) {
                GameLogger.error(TAG, "Sprite at $x, $y is null")
                return@forEach
            }

            if (it == null) {
                GameLogger.error(TAG, "Animation at $x, $y is null")
                return@forEach
            }
            it.update(delta)

            sprite.setRegion(it.getCurrentRegion())
        }
    }
}