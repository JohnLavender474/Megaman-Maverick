package com.mega.game.engine.motion

import com.badlogic.gdx.math.MathUtils.*
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs

class Pendulum(
    var length: Float,
    var gravity: Float,
    var anchor: Vector2,
    var targetFPS: Float,
    var scalar: Float = 1f,
    var defaultAngle: Float = PI / 2f,
    var damping: Float = 0f,
    var disturbanceThreshold: Float = DEFAULT_DISTURBANCE_THRESHOLD,
    var randomForce: () -> Float = { random(-DEFAULT_RANDOM_FORCE, DEFAULT_RANDOM_FORCE) }
) : IMotion {

    companion object {
        const val DEFAULT_DISTURBANCE_THRESHOLD = 0.001f
        const val DEFAULT_RANDOM_FORCE = 0.001f
    }

    var angle = defaultAngle

    private val endPoint = Vector2()
    private var angleVel = 0f
    private var angleAccel = 0f
    private var accumulator = 0f

    fun getPointFromAnchor(distance: Float): Vector2 {
        val point = Vector2()
        point.x = anchor.x + sin(angle) * distance
        point.y = anchor.y + cos(angle) * distance
        return point
    }

    fun applyForce(force: Float) {
        angleVel += force
    }

    fun getSwingDirection() = when {
        angleVel < 0 -> 1
        angleVel > 0 -> -1
        else -> 0
    }

    override fun getMotionValue(out: Vector2): Vector2 = out.set(endPoint)

    override fun update(delta: Float) {
        accumulator += delta
        while (accumulator >= targetFPS) {
            accumulator -= targetFPS
            if (abs(defaultAngle % PI) < disturbanceThreshold) angleVel += randomForce()
            angleAccel = (gravity / length * sin(angle))
            angleVel += angleAccel * targetFPS * scalar
            angleVel *= (1 - (damping * delta))
            angle += angleVel * targetFPS * scalar
        }
        endPoint.set(getPointFromAnchor(length))
    }

    override fun reset() {
        angleVel = 0f
        angleAccel = 0f
        accumulator = 0f
        angle = defaultAngle
        endPoint.setZero()
    }
}
