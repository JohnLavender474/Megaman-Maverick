package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.IAnimator
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.MegaServices
import kotlin.math.min

// TODO: Tiled tile animations run on wall time (AnimatedTiledMapTile) and go on in a pause, entity animations run on game time, so the two cannot be in sync; one level animation clock would drive both (backlog candidate in the 078d branch docs/TODO.md)
object ToxicLiquidClock : Updatable {

    const val FRAMES = 4
    private const val KEY = "ToxicLiquidClock"
    private const val FRAME_DURATION = 0.125f

    private var users = 0
    private var time = 0f

    var frame = 0
        private set

    fun join(services: MegaServices) {
        users++
        if (!services.updatables.containsKey(KEY)) services.updatables.put(KEY, this)
    }

    fun leave(services: MegaServices) {
        check(users > 0) { "$KEY: leave() without a join()" }
        users--
        if (users == 0) services.updatables.remove(KEY)
    }

    override fun update(delta: Float) {
        time = (time + delta) % (FRAMES * FRAME_DURATION)
        frame = min((time / FRAME_DURATION).toInt(), FRAMES - 1)
    }
}

class ToxicLiquidAnimator(private val frames: Array<TextureRegion>) : IAnimator {

    init {
        require(frames.size == ToxicLiquidClock.FRAMES) {
            "ToxicLiquidAnimator needs ${ToxicLiquidClock.FRAMES} frames, got ${frames.size}"
        }
    }

    override fun shouldAnimate(delta: Float) = true

    override fun animate(sprite: GameSprite, delta: Float) = sprite.setRegion(frames[ToxicLiquidClock.frame])

    override fun reset() {}
}
