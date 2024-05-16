package com.megaman.maverick.game.drawables.sprites

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.common.interpolate
import com.engine.common.time.Timer
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection

open class ScrollingBackground(
    region: TextureRegion,
    private val start: Vector2,
    private val target: Vector2,
    duration: Float,
    width: Float,
    height: Float,
    rows: Int,
    cols: Int,
    priority: DrawingPriority = DrawingPriority(DrawingSection.BACKGROUND, 0)
) : Background(start.x, start.y, region, width, height, rows, cols, priority) {

    private val timer = Timer(duration)

    override fun update(delta: Float) {
        super.update(delta)
        timer.update(delta)
        if (timer.isFinished()) {
            backgroundSprites.setPosition(start)
            timer.reset()
        } else {
            val position = interpolate(start, target, timer.getRatio())
            backgroundSprites.setPosition(position)
        }
    }
}