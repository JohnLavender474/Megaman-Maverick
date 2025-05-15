package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.UtilMethods.interpolate
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.utils.GameObjectPools

open class ScrollingBackground(
    game: MegamanMaverickGame,
    key: String,
    region: TextureRegion,
    private val start: Vector2,
    private val target: Vector2,
    duration: Float,
    width: Float,
    height: Float,
    rows: Int,
    cols: Int,
    initPos: Vector2 = Vector2(start).add(width / 2f, height / 2f),
    priority: DrawingPriority = DrawingPriority(DrawingSection.BACKGROUND, 0)
) : Background(game, key, start.x, start.y, region, width, height, rows, cols, priority, initPos = initPos) {

    private val timer = Timer(duration)

    override fun update(delta: Float) {
        super.update(delta)
        timer.update(delta)
        if (timer.isFinished()) {
            backgroundSprites.setPosition(start)
            timer.reset()
        } else {
            val position = interpolate(start, target, timer.getRatio(), GameObjectPools.fetch(Vector2::class))
            backgroundSprites.setPosition(position)
        }
    }
}
