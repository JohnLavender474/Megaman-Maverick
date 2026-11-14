package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstVals

class ClapPlan {
    val meet = Vector2()
    val leftStart = Vector2()
    val leftStop = Vector2()
    val rightStart = Vector2()
    val rightStop = Vector2()
}

object ClapAim {

    const val LEAD = 4f
    const val WALL_GAP = 1f
    const val CEILING_GAP = 1.5f

    fun plan(
        target: Vector2,
        arena: GameRectangle,
        fistHalfWidth: Float,
        floorReach: Float,
        out: ClapPlan
    ): ClapPlan {
        val wall = fistHalfWidth + WALL_GAP * ConstVals.PPM
        val minX = arena.getX() + wall
        val maxX = arena.getMaxX() - wall
        val minY = arena.getY() + floorReach
        val maxY = arena.getMaxY() - CEILING_GAP * ConstVals.PPM
        check(minX <= maxX && minY <= maxY) {
            "ClapAim: the arena $arena is too small for the pincer clap: it must be at least " +
                "${2f * wall / ConstVals.PPM} tiles wide and ${(floorReach / ConstVals.PPM) + CEILING_GAP} tiles high"
        }

        val meet = out.meet.set(MathUtils.clamp(target.x, minX, maxX), MathUtils.clamp(target.y, minY, maxY))
        out.leftStop.set(meet.x - fistHalfWidth, meet.y)
        out.rightStop.set(meet.x + fistHalfWidth, meet.y)

        val lead = LEAD * ConstVals.PPM
        val margin = SlamAim.MARGIN * ConstVals.PPM
        out.leftStart.set(minOf(maxOf(meet.x - lead, arena.getX() + margin), out.leftStop.x), meet.y)
        out.rightStart.set(maxOf(minOf(meet.x + lead, arena.getMaxX() - margin), out.rightStop.x), meet.y)
        return out
    }
}
