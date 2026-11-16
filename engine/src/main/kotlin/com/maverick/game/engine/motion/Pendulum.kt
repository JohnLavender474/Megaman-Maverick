package com.maverick.game.engine.motion

import com.badlogic.gdx.math.MathUtils.PI
import com.badlogic.gdx.math.MathUtils.cos
import com.badlogic.gdx.math.MathUtils.sin
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.random.GameRandom
import kotlin.math.abs

class Pendulum(
    var length: Float,
    var gravity: Float,
    var anchor: Vector2,
    var targetFPS: Float,
    random: GameRandom,
    var scalar: Float = 1f,
    var defaultAngle: Float = PI / 2f,
    var damping: Float = 0f,
    var disturbanceThreshold: Float = DEFAULT_DISTURBANCE_THRESHOLD,
    var randomForce: () -> Float = { random.nextFloat(-DEFAULT_RANDOM_FORCE, DEFAULT_RANDOM_FORCE) }
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

    fun getPointFromAnchor(distance: Float, out: Vector2): Vector2 =
        out.set(anchor.x + sin(angle) * distance, anchor.y + cos(angle) * distance)

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
        getPointFromAnchor(length, endPoint)
    }

    override fun reset() {
        angleVel = 0f
        angleAccel = 0f
        accumulator = 0f
        angle = defaultAngle
        endPoint.setZero()
    }
}
