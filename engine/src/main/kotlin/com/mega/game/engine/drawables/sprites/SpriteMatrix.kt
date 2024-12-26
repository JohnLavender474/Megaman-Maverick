package com.mega.game.engine.drawables.sprites

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sorting.DrawingPriority


data class SpriteMatrixParams(
    val model: TextureRegion,
    val priority: DrawingPriority,
    val modelWidth: Float,
    val modelHeight: Float,
    val rows: Int,
    val columns: Int,
    val x: Float,
    val y: Float
)


class SpriteMatrix(
    model: TextureRegion,
    priority: DrawingPriority,
    private var modelWidth: Float,
    private var modelHeight: Float,
    rows: Int,
    columns: Int
) : IDrawable<Batch>, Matrix<GameSprite>(rows, columns) {

    init {
        for (x in 0 until columns) {
            for (y in 0 until rows) {
                val sprite = GameSprite(model, priority.copy())
                sprite.setSize(modelWidth, modelHeight)
                sprite.setPosition(x * modelWidth, y * modelHeight)
                this[x, y] = sprite
            }
        }
    }


    constructor(
        params: SpriteMatrixParams
    ) : this(
        params.model, params.priority, params.modelWidth, params.modelHeight, params.rows, params.columns
    )


    fun translate(x: Float, y: Float) = forEach { _, _, sprite ->
        (sprite as GameSprite).translate(x, y)
    }


    fun setPosition(startPosition: Vector2) = setPosition(startPosition.x, startPosition.y)


    fun setPosition(startX: Float, startY: Float) {
        forEach { x, y, sprite ->
            sprite?.setPosition(startX + (x * modelWidth), startY + (y * modelHeight))
        }
    }

    override fun draw(drawer: Batch) = forEach { it.draw(drawer) }

    override fun toString(): String {
        return "SpriteMatrix(modelWidth=$modelWidth, modelHeight=$modelHeight, rows=$rows, columns=$columns, matrix=$matrixMap)"
    }
}
