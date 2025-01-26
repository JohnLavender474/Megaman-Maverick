package com.mega.game.engine.common.utils

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2

// https://gamedev.stackexchange.com/questions/100802/in-libgdx-how-might-i-make-an-object-orbit-around-a-position
object OrbitUtils {

    fun calculateOrbitalAngle(
        delta: Float,
        constantSpeed: Float,
        distanceFromCenterPoint: Float,
        currentAngle: Float
    ): Float {
        val angularVelocity = constantSpeed / distanceFromCenterPoint

        var newAngle = currentAngle + angularVelocity * delta
        while (newAngle >= 360f) newAngle -= 360f
        while (newAngle < 0f) newAngle += 360f

        return newAngle
    }

    fun calculateOrbitalPosition(angle: Float, distance: Float, centerPoint: Vector2, out: Vector2): Vector2 {
        val radians = angle * MathUtils.degreesToRadians

        val x = (MathUtils.cos(radians) * distance) + centerPoint.x
        val y = (MathUtils.sin(radians) * distance) + centerPoint.y

        return out.set(x, y)
    }
}
