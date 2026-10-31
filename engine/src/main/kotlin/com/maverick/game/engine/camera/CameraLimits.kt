package com.maverick.game.engine.camera

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import kotlin.math.max
import kotlin.math.min

class CameraAxisLimit {

    var min = Float.NEGATIVE_INFINITY
        private set
    var max = Float.POSITIVE_INFINITY
        private set

    var pinned = false
        private set

    private var pin = 0f

    fun set(min: Float, max: Float) {
        this.min = min
        this.max = max
        pinned = false
    }

    fun setMin(min: Float) {
        this.min = min
        pinned = false
    }

    fun setMax(max: Float) {
        this.max = max
        pinned = false
    }

    fun setPin(pin: Float) {
        this.pin = pin
        pinned = true
    }

    fun set(other: CameraAxisLimit) {
        min = other.min
        max = other.max
        pinned = other.pinned
        pin = other.pin
    }

    fun clamp(center: Float, viewSize: Float): Float {
        var result = center
        if (result > max - viewSize / 2f) result = max - viewSize / 2f
        if (result < min + viewSize / 2f) result = min + viewSize / 2f
        return result
    }

    fun resolve(viewSize: Float, outerMin: Float, outerMax: Float) {
        val low = if (pinned) pin - viewSize / 2f else min
        val high = if (pinned) pin + viewSize / 2f else max
        this.min = max(low, outerMin)
        this.max = min(high, outerMax)
    }

    fun easeTo(target: CameraAxisLimit, center: Float, viewSize: Float, step: Float) {
        if (target.pinned) {
            set(target)
            return
        }

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
    val keptEdges = ObjectSet<Direction>()
    val killEdges = ObjectSet<Direction>()
    val edgeExtents = ObjectMap<Direction, Rectangle>()
    val crushSurfaces = Array<GameRectangle>()

    fun clearEdges() {
        blockedEdges.clear()
        keptEdges.clear()
        killEdges.clear()
        edgeExtents.clear()
        crushSurfaces.clear()
    }

    fun set(other: CameraLimits) {
        x.set(other.x)
        y.set(other.y)
        easeSpeed = other.easeSpeed
        clearEdges()
        blockedEdges.addAll(other.blockedEdges)
        keptEdges.addAll(other.keptEdges)
        killEdges.addAll(other.killEdges)
        edgeExtents.putAll(other.edgeExtents)
        crushSurfaces.addAll(other.crushSurfaces)
    }
}
