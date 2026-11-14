package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstVals

enum class OrbAim {
    LEAD,
    CURRENT,
    NEAR,
    WIDE;

    companion object {
        const val LEAD_TIME = 1.25f
        const val LEAD_MAX_HEIGHT = 3f
        const val NEAR_SPREAD = 1.5f
        const val WIDE_MIN = 3f
        const val WIDE_MAX = 5f
        const val WIDE_HEIGHT = 1f

        fun point(
            aim: OrbAim,
            playerCenter: Vector2,
            playerVelocity: Vector2,
            arena: GameRectangle,
            random: (Float, Float) -> Float,
            out: Vector2
        ): Vector2 {
            val ppm = ConstVals.PPM
            val floor = arena.getY()
            when (aim) {
                LEAD -> out.set(
                    playerCenter.x + playerVelocity.x * LEAD_TIME,
                    MathUtils.clamp(playerCenter.y, floor, floor + LEAD_MAX_HEIGHT * ppm)
                )

                CURRENT -> out.set(playerCenter)
                NEAR -> out.set(playerCenter.x + random(-NEAR_SPREAD, NEAR_SPREAD) * ppm, playerCenter.y)
                WIDE -> {
                    val side = if (random(0f, 1f) < 0.5f) -1f else 1f
                    out.set(playerCenter.x + side * random(WIDE_MIN, WIDE_MAX) * ppm, floor + WIDE_HEIGHT * ppm)
                }
            }
            out.x = MathUtils.clamp(out.x, arena.getX(), arena.getMaxX())
            out.y = MathUtils.clamp(out.y, arena.getY(), arena.getMaxY())
            return out
        }
    }
}
