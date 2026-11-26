package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.enums.Direction
import com.megaman.maverick.game.ConstVals
import kotlin.math.sqrt

object AxeTrajectory {

    const val GRAVITY = 56.25f

    fun gravity(direction: Direction, gravityScalar: Float, out: Vector2): Vector2 {
        val gravity = GRAVITY * gravityScalar * ConstVals.PPM
        return when (direction) {
            Direction.UP -> out.set(0f, -gravity)
            Direction.DOWN -> out.set(0f, gravity)
            Direction.LEFT -> out.set(gravity, 0f)
            Direction.RIGHT -> out.set(-gravity, 0f)
        }
    }

    fun dots(
        start: Vector2,
        velocity: Vector2,
        gravity: Vector2,
        up: Direction,
        fixedStep: Float,
        spacing: Float,
        maxDots: Int,
        maxTime: Float,
        out: Array<Vector2>
    ): Int {
        require(fixedStep > 0f) { "fixedStep must be greater than 0, was $fixedStep" }
        require(spacing > 0f) { "spacing must be greater than 0, was $spacing" }
        require(maxDots <= out.size) { "out holds ${out.size} vectors, fewer than maxDots=$maxDots" }

        val upX = when (up) {
            Direction.LEFT -> -1f
            Direction.RIGHT -> 1f
            else -> 0f
        }
        val upY = when (up) {
            Direction.UP -> 1f
            Direction.DOWN -> -1f
            else -> 0f
        }
        val minHeight = -ConstVals.PPM.toFloat()
        var x = start.x
        var y = start.y
        var vx = velocity.x
        var vy = velocity.y
        var time = 0f
        var travelled = 0f
        var nextDotAt = spacing
        var count = 0

        while (count < maxDots && time < maxTime) {
            vx += gravity.x * fixedStep
            vy += gravity.y * fixedStep
            val nextX = x + vx * fixedStep
            val nextY = y + vy * fixedStep
            time += fixedStep

            val height = (nextX - start.x) * upX + (nextY - start.y) * upY
            if (height < minHeight) break

            val dx = nextX - x
            val dy = nextY - y
            val segment = sqrt(dx * dx + dy * dy)
            while (count < maxDots && segment > 0f && travelled + segment >= nextDotAt) {
                val fraction = (nextDotAt - travelled) / segment
                out[count].set(x + dx * fraction, y + dy * fraction)
                count++
                nextDotAt += spacing
            }

            travelled += segment
            x = nextX
            y = nextY
        }

        return count
    }
}
