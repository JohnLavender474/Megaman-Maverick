package com.megaman.maverick.game.entities.bosses.d1000

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.random.GameRandom
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.entities.megamanx.MegamanXValues
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

enum class D1000BombPattern {
    AIMED, BRACKET, SWEEP, FENCE, WALL;

    companion object {
        fun of(action: D1000Action) = when (action) {
            D1000Action.BRACKET -> BRACKET
            D1000Action.SWEEP -> SWEEP
            D1000Action.FENCE -> FENCE
            D1000Action.WALL -> WALL
            else -> AIMED
        }
    }
}

enum class D1000Role { HUNT, CUT, FEINT, SWEEP, HERD }

// TODO: the D-1000 fight (its behaviors, mechanics, tuning and anti-trap rules) was AI-generated from owner play tests; it needs more scrutiny and refinement (see the branch NOTES of 081f, section "AI-generated fight")
object D1000Orbs {

    const val LANE_MARGIN = 0.5f * ConstVals.PPM
    const val LANE_TOLERANCE = 0.01f * ConstVals.PPM
    const val MARK_DUR = 0.6f
    const val GUIDE_SPEED = 16f * ConstVals.PPM
    const val GUIDE_FLIGHT_MAX = 1.2f
    const val CUT_GAP = 3f * ConstVals.PPM
    const val FEINT_MIN = 2f * ConstVals.PPM
    const val FEINT_MAX = 3.5f * ConstVals.PPM
    const val FEINT_SPREAD = 3f * ConstVals.PPM
    const val BODY_LANE_MARGIN = 0.5f * ConstVals.PPM
    const val FEINT_TRIES = 8
    const val STILL_SPEED = 0.5f * ConstVals.PPM
    const val FENCE_SPACING = 4f * ConstVals.PPM
    const val FENCE_WALL_POCKET = 2f * ConstVals.PPM
    const val MAX_LANES = 16
    const val PATTERN_END_AFTER = 1.1f
    const val WALL_COUNT = 3
    const val WALL_EYE_GAP = 2.5f * ConstVals.PPM
    const val WALL_FLOOR_CLEAR = (MegamanXValues.BODY_HEIGHT + 0.75f) * ConstVals.PPM + D1000Pillar.WIDTH / 2f
    const val WALL_BODY_CLEAR = 0.25f * ConstVals.PPM + D1000Pillar.WIDTH / 2f
    const val CUT_HEIGHT = 2.5f * ConstVals.PPM

    private val H = D1000Role.HUNT
    private val C = D1000Role.CUT
    private val F = D1000Role.FEINT

    private val SCRIPTS = mapOf(
        D1000BombPattern.BRACKET to arrayOf(arrayOf(H, C, H), arrayOf(C, H, F), arrayOf(H, F, C)),
        D1000BombPattern.SWEEP to arrayOf(arrayOf(C, H, C, H), arrayOf(H, C, C, H)),
        D1000BombPattern.AIMED to arrayOf(arrayOf(H, F, H, F, H)),
        D1000BombPattern.WALL to arrayOf(arrayOf(H, C, H), arrayOf(C, H, C))
    )

    fun laneInArena(arena: GameRectangle, x: Float) =
        x >= arena.getX() + LANE_MARGIN - LANE_TOLERANCE && x <= arena.getMaxX() - LANE_MARGIN + LANE_TOLERANCE

    fun clampLane(arena: GameRectangle, x: Float) = x.coerceIn(arena.getX() + LANE_MARGIN, arena.getMaxX() - LANE_MARGIN)

    fun aim(arena: GameRectangle, playerX: Float, playerSpeedX: Float, tuning: D1000Tuning): Float {
        val lead = (playerSpeedX * tuning.leadTime).coerceIn(-tuning.leadMax, tuning.leadMax)
        return clampLane(arena, playerX + lead)
    }

    fun aimHeight(playerY: Float, playerSpeedY: Float, tuning: D1000Tuning, low: Float, high: Float): Float {
        val lead = (playerSpeedY * tuning.leadTime).coerceIn(-tuning.leadMax, tuning.leadMax)
        return (playerY + lead).coerceIn(low, high)
    }

    fun cutHeight(playerY: Float, playerSpeedY: Float, low: Float, high: Float): Float {
        val side = when {
            playerSpeedY > STILL_SPEED -> 1
            playerSpeedY < -STILL_SPEED -> -1
            high - playerY >= playerY - low -> 1
            else -> -1
        }
        val first = playerY + side * CUT_HEIGHT
        val second = playerY - side * CUT_HEIGHT
        return (if (first in low..high || second !in low..high) first else second).coerceIn(low, high)
    }

    fun wallX(arena: GameRectangle, playerX: Float) =
        if (playerX < arena.getX() + arena.getWidth() / 2f) arena.getMaxX() else arena.getX()

    fun scriptCount(pattern: D1000BombPattern) = SCRIPTS[pattern]?.size ?: 1

    fun count(pattern: D1000BombPattern, tuning: D1000Tuning) = when (pattern) {
        D1000BombPattern.AIMED -> tuning.bombCount
        D1000BombPattern.BRACKET -> tuning.bracketCount
        D1000BombPattern.SWEEP -> tuning.sweepCount
        D1000BombPattern.WALL -> WALL_COUNT
        D1000BombPattern.FENCE -> throw IllegalArgumentException("D-1000 Fence counts its lanes, not a tuning count")
    }

    fun roles(pattern: D1000BombPattern, script: Int, count: Int, out: Array<D1000Role>): Array<D1000Role> {
        out.clear()
        if (pattern == D1000BombPattern.FENCE) {
            for (lane in 0 until count) {
                out.add(D1000Role.SWEEP)
                if (lane < count - 1) out.add(D1000Role.HUNT)
            }
            return out
        }
        val base = SCRIPTS[pattern]!![script]
        for (i in 0 until count) out.add(if (i < base.size) base[i] else D1000Role.HUNT)
        return out
    }

    fun cutSide(arena: GameRectangle, playerX: Float, playerSpeedX: Float): Int = when {
        playerSpeedX > STILL_SPEED -> 1
        playerSpeedX < -STILL_SPEED -> -1
        arena.getMaxX() - playerX >= playerX - arena.getX() -> 1
        else -> -1
    }

    fun sideLane(arena: GameRectangle, playerX: Float, side: Int, bodyLow: Float, bodyHigh: Float): Float {
        val ahead = playerX + side * CUT_GAP
        val behind = playerX - side * CUT_GAP
        return clampLane(arena, when {
            open(arena, ahead, bodyLow, bodyHigh) -> ahead
            open(arena, behind, bodyLow, bodyHigh) -> behind
            laneInArena(arena, ahead) -> ahead
            else -> behind
        })
    }

    fun feintLane(
        arena: GameRectangle,
        playerX: Float,
        used: FloatArray,
        usedCount: Int,
        bodyLow: Float,
        bodyHigh: Float,
        random: GameRandom
    ): Float {
        repeat(FEINT_TRIES) {
            val side = if (random.nextBoolean()) 1 else -1
            val x = playerX + side * (FEINT_MIN + random.nextFloat(0f, 1f) * (FEINT_MAX - FEINT_MIN))
            if (open(arena, x, bodyLow, bodyHigh) && spread(x, used, usedCount)) return x
        }
        val roomy = if (arena.getMaxX() - playerX >= playerX - arena.getX()) 1 else -1
        for (side in intArrayOf(roomy, -roomy)) for (gap in floatArrayOf(FEINT_MAX, FEINT_MIN)) {
            val x = playerX + side * gap
            if (open(arena, x, bodyLow, bodyHigh) && spread(x, used, usedCount)) return x
        }
        return Float.NaN
    }

    fun nearLane(arena: GameRectangle, playerX: Float, bodyLow: Float, bodyHigh: Float): Float {
        val roomy = if (arena.getMaxX() - playerX >= playerX - arena.getX()) 1 else -1
        for (side in intArrayOf(roomy, -roomy)) {
            val x = playerX + side * FEINT_MIN
            if (open(arena, x, bodyLow, bodyHigh)) return x
        }
        return clampLane(arena, playerX + roomy * FEINT_MIN)
    }

    fun underBody(x: Float, bodyLow: Float, bodyHigh: Float) = x > bodyLow && x < bodyHigh

    private fun open(arena: GameRectangle, x: Float, bodyLow: Float, bodyHigh: Float) =
        laneInArena(arena, x) && !underBody(x, bodyLow, bodyHigh)

    private fun spread(x: Float, used: FloatArray, usedCount: Int): Boolean {
        for (i in 0 until usedCount) if (abs(x - used[i]) < FEINT_SPREAD) return false
        return true
    }

    fun nearestLane(lanes: FloatArray, count: Int, x: Float): Float {
        var best = lanes[0]
        for (i in 1 until count) if (abs(lanes[i] - x) < abs(best - x)) best = lanes[i]
        return best
    }

    fun fenceLane(arena: GameRectangle, x: Float): Boolean {
        if (!laneInArena(arena, x)) return false
        val pocket = min(x - arena.getX(), arena.getMaxX() - x) - D1000Pillar.WIDTH / 2f
        return pocket < 2f * D1000XMoves.HALF_WIDTH || pocket >= FENCE_WALL_POCKET - LANE_TOLERANCE
    }

    fun fenceLanes(arena: GameRectangle, anchorX: Float, out: FloatArray): Int {
        val low = arena.getX() + LANE_MARGIN
        val high = arena.getMaxX() - LANE_MARGIN
        val gaps = floor((high - low) / FENCE_SPACING).toInt()
        check(gaps >= 1) { "D-1000 Pillar Fence needs an arena at least ${FENCE_SPACING + 2f * LANE_MARGIN} wide" }
        val spacing = (high - low) / gaps
        val anchor = anchorX.coerceIn(low, high)
        val tolerance = LANE_TOLERANCE / spacing
        val kMin = ceil((low - anchor) / spacing - tolerance).toInt()
        val kMax = floor((high - anchor) / spacing + tolerance).toInt()
        val far = if (kMax >= -kMin) 1 else -1
        var count = 0
        fun add(k: Int) {
            val x = anchor + k * spacing
            if (k < kMin || k > kMax || !fenceLane(arena, x)) return
            check(count < out.size) { "D-1000 Pillar Fence has more than ${out.size} lanes: raise MAX_LANES" }
            out[count++] = x
        }
        add(0)
        for (k in 1..maxOf(kMax, -kMin)) add(far * k)
        for (k in 1..maxOf(kMax, -kMin)) add(-far * k)
        check(count > 0) { "D-1000 Pillar Fence found no lane in the arena" }
        return count
    }
}
