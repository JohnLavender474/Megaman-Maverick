package com.megaman.maverick.game.entities.bosses.d1000

import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megamanx.MegamanXValues
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

// TODO: the D-1000 fight (its behaviors, mechanics, tuning and anti-trap rules) was AI-generated from owner play tests; it needs more scrutiny and refinement (see the branch NOTES of 081f, section "AI-generated fight")
class D1000Danger {
    var left = 0f
    var right = 0f
    var from = 0f
    var to = 0f
    var jumpable = false
    var stun = false
    var fullHeight = false
    var wallOnly = false
}

class D1000Moves(val speed: Float, val reaction: Float, val timedReaction: Float, val margin: Float) {

    companion object {
        val NORMAL = D1000Moves(MegamanXValues.WALK_SPEED * ConstVals.PPM, 0.25f, 0.1f, 0.0625f * ConstVals.PPM)
        val HARD = D1000Moves(MegamanXValues.WALK_SPEED * ConstVals.PPM, 0.15f, 0.05f, 0f)
    }
}

object D1000XMoves {

    const val HALF_WIDTH = MegamanXValues.BODY_WIDTH * ConstVals.PPM / 2f
    const val HEIGHT = MegamanXValues.BODY_HEIGHT * ConstVals.PPM
    private const val STEP = 1f / 120f
    private const val MAX_CLIMB = 5f

    fun fallTime(drop: Float) = sqrt(2f * max(drop, 0f) / (-MegamanXValues.FALL_GRAVITY * ConstVals.PPM))

    fun climbTime(height: Float): Float {
        val target = height / ConstVals.PPM
        var time = 0f
        var y = 0f
        var speed = MegamanXValues.JUMP_SPEED
        while (time < MAX_CLIMB) {
            speed += (if (speed > 0f) MegamanXValues.JUMP_GRAVITY else MegamanXValues.FALL_GRAVITY) * STEP
            y += speed * STEP
            time += STEP
            if (y >= target) return time
            if (speed <= 0f) {
                time += MegamanXValues.WALL_GRAB_TIME
                y -= MegamanXValues.WALL_SLIDE_SPEED * MegamanXValues.WALL_GRAB_TIME
                speed = MegamanXValues.WALL_KICK_SPEED
            }
        }
        return Float.POSITIVE_INFINITY
    }
}

class D1000Escape {

    companion object {
        const val STEP = 1f / 60f
        const val HORIZON = 4f
        const val MAX_DANGERS = 1024
        private val STEPS = (HORIZON / STEP).roundToInt()
    }

    private val dangers = Array<D1000Danger>()
    var count = 0
        private set

    private var reach = BooleanArray(0)
    private var next = BooleanArray(0)

    fun clear() {
        count = 0
    }

    fun rollBack(mark: Int) {
        count = mark
    }

    fun danger(index: Int): D1000Danger = dangers[index]

    fun add(
        left: Float, right: Float, from: Float, to: Float, jumpable: Boolean = false, fullHeight: Boolean = false,
        wallOnly: Boolean = false
    ) {
        if (to < max(from, 0f) || right < left) return
        push(left, right, max(from, 0f), to, jumpable, false, fullHeight || wallOnly)
        dangers[count - 1].wallOnly = wallOnly
    }

    fun stun(from: Float, to: Float) {
        if (to <= max(from, 0f)) return
        push(0f, 0f, max(from, 0f), to, false, true, false)
    }

    private fun push(
        left: Float, right: Float, from: Float, to: Float, jumpable: Boolean, stun: Boolean, fullHeight: Boolean
    ) {
        check(count < MAX_DANGERS) { "D-1000 tracks more than $MAX_DANGERS dangers: raise MAX_DANGERS or coarsen the slices" }
        if (count == dangers.size) dangers.add(D1000Danger())
        val danger = dangers[count++]
        danger.left = left
        danger.right = right
        danger.from = from
        danger.to = to
        danger.jumpable = jumpable
        danger.stun = stun
        danger.fullHeight = fullHeight
        danger.wallOnly = false
    }

    fun stunnedAt(time: Float): Boolean {
        for (i in 0 until count) {
            val danger = dangers[i]
            if (danger.stun && time >= danger.from && time <= danger.to) return true
        }
        return false
    }

    fun free(left: Float, right: Float, from: Float, to: Float): Boolean {
        for (i in 0 until count) {
            val danger = dangers[i]
            if (!danger.fullHeight) continue
            if (danger.to < from || danger.from > to) continue
            if (danger.right > left && danger.left < right) return false
        }
        return true
    }

    fun hasEscape(x: Float, minX: Float, maxX: Float, moves: D1000Moves, start: Float = 0f): Boolean {
        val low = minX + D1000XMoves.HALF_WIDTH
        val high = maxX - D1000XMoves.HALF_WIDTH
        if (high < low) return false
        val cell = moves.speed * STEP
        val cells = floor((high - low) / cell).toInt() + 1
        if (reach.size < cells) {
            reach = BooleanArray(cells)
            next = BooleanArray(cells)
        }
        reach.fill(false, 0, cells)
        val first = ((x - low) / cell).roundToInt().coerceIn(0, cells - 1)
        reach[first] = true
        val startX = low + first * cell
        val pad = D1000XMoves.HALF_WIDTH + moves.margin

        val react = start + moves.reaction
        for (step in ceil(start / STEP).toInt() + 1..STEPS) {
            val time = step * STEP
            val stunned = stunnedAt(time)
            if (time > react && !stunned) {
                for (i in 0 until cells) next[i] = reach[i] || (i > 0 && reach[i - 1]) || (i < cells - 1 && reach[i + 1])
                System.arraycopy(next, 0, reach, 0, cells)
            }
            for (d in 0 until count) {
                val danger = dangers[d]
                if (danger.stun || danger.wallOnly || time < danger.from || time > danger.to) continue
                if (danger.jumpable && !stunned) continue
                if (danger.from <= react && startX + pad > danger.left && startX - pad < danger.right) continue
                val from = max(0, floor((danger.left - pad - low) / cell).toInt() + 1)
                val to = min(cells - 1, ceil((danger.right + pad - low) / cell).toInt() - 1)
                for (i in from..to) reach[i] = false
            }
            var any = false
            for (i in 0 until cells) if (reach[i]) {
                any = true
                break
            }
            if (!any) return false
        }
        return true
    }
}
