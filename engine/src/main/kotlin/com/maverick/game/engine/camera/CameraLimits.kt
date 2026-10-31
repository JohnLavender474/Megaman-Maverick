package com.maverick.game.engine.camera

import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.enums.Direction
import kotlin.math.max
import kotlin.math.min

class CameraAxisLimit {

    var min = Float.NEGATIVE_INFINITY
        private set
    var max = Float.POSITIVE_INFINITY
        private set

    fun set(min: Float, max: Float) {
        this.min = min
        this.max = max
    }

    fun setMin(min: Float) {
        this.min = min
    }

    fun setMax(max: Float) {
        this.max = max
    }

    fun set(other: CameraAxisLimit) {
        min = other.min
        max = other.max
    }

    fun clamp(center: Float, viewSize: Float): Float {
        var result = center
        if (result > max - viewSize / 2f) result = max - viewSize / 2f
        if (result < min + viewSize / 2f) result = min + viewSize / 2f
        return result
    }

    fun resolve(outerMin: Float, outerMax: Float) {
        set(max(min, outerMin), min(max, outerMax))
    }

    fun easeTo(target: CameraAxisLimit, center: Float, viewSize: Float, step: Float) {
        val low = when {
            target.min > min -> min(target.min, max(min, center - viewSize / 2f) + step)
            else -> max(target.min, min - step)
        }
        val high = when {
            target.max < max -> max(target.max, min(max, center + viewSize / 2f) - step)
            else -> min(target.max, max + step)
        }
        set(low, high)
    }
}

class CameraLimits {

    val x = CameraAxisLimit()
    val y = CameraAxisLimit()

    var easeSpeed = 0f

    val blockedEdges = ObjectSet<Direction>()

    fun set(other: CameraLimits) {
        x.set(other.x)
        y.set(other.y)
        easeSpeed = other.easeSpeed
        blockedEdges.clear()
        blockedEdges.addAll(other.blockedEdges)
    }
}
