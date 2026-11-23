package com.maverick.game.engine.common.shapes

import com.badlogic.gdx.math.Vector2
import kotlin.math.max
import kotlin.math.min

object CeilingProfile {

    fun lowestCeiling(lowerChain: FloatArray, count: Int, x0: Float, x1: Float): Float {
        val start = max(x0, lowerChain[0])
        val end = min(x1, lowerChain[2 * (count - 1)])
        if (start > end) return Float.POSITIVE_INFINITY

        var lowest = min(heightAt(lowerChain, count, start), heightAt(lowerChain, count, end))
        for (i in 0 until count) {
            val x = lowerChain[2 * i]
            if (x > start && x < end) lowest = min(lowest, lowerChain[2 * i + 1])
        }
        return lowest
    }

    fun spanBelow(lowerChain: FloatArray, count: Int, y: Float, out: Vector2): Boolean {
        var first = -1
        for (i in 0 until count) if (lowerChain[2 * i + 1] < y) {
            first = i
            break
        }
        if (first == -1) return false

        var last = first
        for (i in count - 1 downTo first) if (lowerChain[2 * i + 1] < y) {
            last = i
            break
        }

        val left = if (first == 0) lowerChain[0] else crossingX(lowerChain, first - 1, first, y)
        val right = if (last == count - 1) lowerChain[2 * last] else crossingX(lowerChain, last, last + 1, y)
        out.set(left, right)
        return true
    }

    private fun heightAt(lowerChain: FloatArray, count: Int, x: Float): Float {
        for (i in 1 until count) {
            val x2 = lowerChain[2 * i]
            if (x <= x2) {
                val x1 = lowerChain[2 * (i - 1)]
                val y1 = lowerChain[2 * (i - 1) + 1]
                val y2 = lowerChain[2 * i + 1]
                if (x2 == x1) return min(y1, y2)
                return y1 + (y2 - y1) * (x - x1) / (x2 - x1)
            }
        }
        return lowerChain[2 * (count - 1) + 1]
    }

    private fun crossingX(lowerChain: FloatArray, i: Int, j: Int, y: Float): Float {
        val x1 = lowerChain[2 * i]
        val y1 = lowerChain[2 * i + 1]
        val x2 = lowerChain[2 * j]
        val y2 = lowerChain[2 * j + 1]
        if (y2 == y1) return x1
        return x1 + (x2 - x1) * (y - y1) / (y2 - y1)
    }
}
