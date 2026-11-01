package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.maverick.game.engine.common.enums.Direction

class CameraRotation(var onFinished: (() -> Unit)? = null) : ICameraPart {

    companion object {
        private val interpolation = Interpolation.smooth
    }

    var direction = Direction.UP
        private set

    var angle = 0f
        private set

    var rotating = false
        private set

    val sideways: Float
        get() {
            val end = if (direction.isHorizontal()) 1f else 0f
            if (!rotating) return end
            return interpolation.apply(startSideways, end, MathUtils.clamp(blendTime / blendDuration, 0f, 1f))
        }

    private var startSideways = 0f
    private var blendTime = 0f
    private var blendDuration = 0f

    private var accumulator = 0f
    private var startAngle = 0f
    private var endAngle = 0f
    private var duration = 0f

    override fun update(camera: Camera, delta: Float): Boolean {
        step(delta)
        return false
    }

    fun step(delta: Float) {
        if (!rotating) return

        blendTime += delta
        accumulator += delta
        val alpha = MathUtils.clamp(accumulator / duration, 0f, 1f)

        angle += interpolation.apply(startAngle, endAngle, alpha) - angle

        if (MathUtils.isEqual(alpha, 1f, 0.0001f)) {
            accumulator = 0f
            rotating = false
            onFinished?.invoke()
        }
    }

    fun start(direction: Direction, time: Float) {
        startSideways = sideways
        blendTime = 0f
        blendDuration = time - accumulator

        this.direction = direction

        startAngle = angle
        endAngle = startAngle + shortestRotation(startAngle, direction.rotation)
        duration = time
        rotating = true
    }

    fun snap(direction: Direction) {
        this.direction = direction
        angle += direction.rotation - angle
        rotating = false
        accumulator = 0f
    }

    private fun shortestRotation(current: Float, target: Float): Float {
        var delta = (target - current) % 360
        if (delta < -180) delta += 360
        if (delta > 180) delta -= 360
        return delta
    }
}
