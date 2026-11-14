package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstVals

class SlamPlan {
    val direction = Vector2()
    val start = Vector2()
    val stop = Vector2()
    val hit = Vector2()
    var surface = ArenaSide.FLOOR
}

object SlamAim {

    const val LEAD = 4f
    const val MARGIN = 1.5f

    private const val MIN_AHEAD_GAP = 1f
    private const val EPSILON = 0.001f
    private const val TIE = 0.001f

    private val DIRECTIONS: Array<Vector2> = gdxArrayOf(
        Vector2(0f, -1f),
        Vector2(-1f, -1f).nor(),
        Vector2(1f, -1f).nor(),
        Vector2(-1f, 0f),
        Vector2(1f, 0f)
    )

    private val candidate = Vector2()

    fun plan(
        fist: Vector2,
        target: Vector2,
        arena: GameRectangle,
        sideReach: Float,
        floorReach: Float,
        out: SlamPlan
    ): SlamPlan {
        val epsilon = EPSILON * ConstVals.PPM
        val tie = TIE * ConstVals.PPM * ConstVals.PPM
        val minAhead = sideReach + MIN_AHEAD_GAP * ConstVals.PPM
        var best = -1
        var bestDistance = Float.MAX_VALUE
        for (i in 0 until DIRECTIONS.size) {
            val direction = DIRECTIONS[i]
            startFor(direction, target, arena, candidate)
            val ahead = (target.x - candidate.x) * direction.x + (target.y - candidate.y) * direction.y
            if (ahead < minAhead - epsilon) continue
            val distance = candidate.dst2(fist)
            if (distance < bestDistance - tie) {
                best = i
                bestDistance = distance
            }
        }
        check(best >= 0) {
            "SlamAim: no dive direction has the target $target at least ${minAhead / ConstVals.PPM} tiles ahead " +
                "of its start in the arena $arena. The arena is too small for the slam: make it wider and taller " +
                "than the $MARGIN tile margin on each side plus that distance"
        }

        val direction = DIRECTIONS[best]
        out.direction.set(direction)
        startFor(direction, target, arena, out.start)

        val start = out.start
        val tx = when {
            direction.x < 0f -> (arena.getX() + sideReach - start.x) / direction.x
            direction.x > 0f -> (arena.getMaxX() - sideReach - start.x) / direction.x
            else -> Float.MAX_VALUE
        }
        val ty = if (direction.y < 0f) (arena.getY() + floorReach - start.y) / direction.y else Float.MAX_VALUE
        out.surface = when {
            ty <= tx + epsilon -> ArenaSide.FLOOR
            direction.x < 0f -> ArenaSide.LEFT_WALL
            else -> ArenaSide.RIGHT_WALL
        }
        val t = if (out.surface == ArenaSide.FLOOR) ty else tx
        out.stop.set(direction).scl(t).add(start)
        when (out.surface) {
            ArenaSide.FLOOR -> out.hit.set(out.stop.x, arena.getY())
            ArenaSide.LEFT_WALL -> out.hit.set(arena.getX(), out.stop.y)
            ArenaSide.RIGHT_WALL -> out.hit.set(arena.getMaxX(), out.stop.y)
        }
        return out
    }

    private fun startFor(direction: Vector2, target: Vector2, arena: GameRectangle, out: Vector2): Vector2 {
        val margin = MARGIN * ConstVals.PPM
        val minX = arena.getX() + margin
        val maxX = arena.getMaxX() - margin
        val minY = arena.getY() + margin
        val maxY = arena.getMaxY() - margin
        var lead = LEAD * ConstVals.PPM
        if (direction.x > 0f) lead = minOf(lead, (target.x - minX) / direction.x)
        if (direction.x < 0f) lead = minOf(lead, (maxX - target.x) / -direction.x)
        if (direction.y < 0f) lead = minOf(lead, (maxY - target.y) / -direction.y)
        lead = maxOf(lead, 0f)
        out.set(direction).scl(-lead).add(target)
        out.x = MathUtils.clamp(out.x, minX, maxX)
        out.y = MathUtils.clamp(out.y, minY, maxY)
        return out
    }
}
