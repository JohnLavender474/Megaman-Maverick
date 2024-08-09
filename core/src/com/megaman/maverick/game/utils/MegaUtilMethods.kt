package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Vector2
import com.megaman.maverick.game.ConstVals

object MegaUtilMethods {

    fun vector2FromString(input: String): Vector2 {
        val parsed = input.replace("\\s+", "").split(",")
        val x = parsed[0].toFloat()
        val y = parsed[1].toFloat()
        return Vector2(x, y)
    }

    fun getDefaultFontSize() = Math.round(ConstVals.PPM / 2f)

    fun calculateJumpImpulse(
        source: Vector2,
        target: Vector2,
        verticalBaseImpulse: Float,
        horizontalScalar: Float = 1f,
        verticalScalar: Float = 1f
    ) = calculateJumpImpulse(
        source.x,
        source.y,
        target.x,
        target.y,
        horizontalScalar,
        verticalBaseImpulse,
        verticalScalar
    )

    fun calculateJumpImpulse(
        sourceX: Float, sourceY: Float,
        targetX: Float, targetY: Float,
        horizontalScalar: Float, verticalBaseImpulse: Float,
        verticalScalar: Float
    ): Vector2 {
        val horizontalDistance = targetX - sourceX
        val verticalDistance = targetY - sourceY

        val impulseX = horizontalDistance * horizontalScalar
        val impulseY = verticalBaseImpulse + (verticalDistance * verticalScalar)

        return Vector2(impulseX, impulseY)
    }
}
