package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.interfaces.ICopyable
import kotlin.math.pow

class ArcMotion(
    startPosition: Vector2,
    targetPosition: Vector2,
    var speed: Float,
    var arcFactor: Float,
    var continueBeyondTarget: Boolean = false
) : IMotion, ICopyable<ArcMotion> {

    companion object {

        fun computeBezierPoint(t: Float, arcFactor: Float, startPosition: Vector2, targetPosition: Vector2): Vector2 {
            val controlPoint = Vector2(
                ((startPosition.x + targetPosition.x) / 2f) + (arcFactor * (targetPosition.y - startPosition.y)),
                ((startPosition.y + targetPosition.y) / 2f) + (arcFactor * (startPosition.x - targetPosition.x))
            )

            val x =
                ((1 - t).pow(2) * startPosition.x) + (2 * (1 - t) * t * controlPoint.x) + (t.pow(2) * targetPosition.x)
            val y =
                ((1 - t).pow(2) * startPosition.y) + (2 * (1 - t) * t * controlPoint.y) + (t.pow(2) * targetPosition.y)

            return Vector2(x, y)
        }
    }

    var startPosition: Vector2 = startPosition.cpy()
        set(value) {
            field = value.cpy()
            reset()
        }
    var targetPosition: Vector2 = targetPosition.cpy()
        set(value) {
            field = value.cpy()
            reset()
        }
    var distanceCovered = 0f
        private set
    val totalDistance: Float
        get() = startPosition.dst(targetPosition)

    private var currentPosition = startPosition.cpy()

    override fun update(delta: Float) {
        distanceCovered += speed * delta

        if (!continueBeyondTarget && distanceCovered >= totalDistance) {
            currentPosition = targetPosition.cpy()
            return
        }

        val t = distanceCovered / totalDistance
        currentPosition = computeBezierPoint(t, arcFactor, startPosition, targetPosition)
    }

    fun compute(t: Float) = computeBezierPoint(t, arcFactor, startPosition, targetPosition)

    override fun getMotionValue(out: Vector2): Vector2? = out.set(currentPosition)

    override fun reset() {
        currentPosition = startPosition.cpy()
        distanceCovered = 0f
    }

    override fun copy() = ArcMotion(startPosition.cpy(), targetPosition.cpy(), speed, arcFactor, continueBeyondTarget)
}