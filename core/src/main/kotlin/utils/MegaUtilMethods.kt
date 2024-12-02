package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Vector2
import com.megaman.maverick.game.ConstVals
import kotlin.math.roundToInt

object MegaUtilMethods {

    fun getSmallFontSize() = (ConstVals.PPM / 3f).roundToInt()

    fun getDefaultFontSize() = (ConstVals.PPM / 2f).roundToInt()

    fun getLargeFontSize() = (ConstVals.PPM / 1.5f).roundToInt()

    fun calculateJumpImpulse(
        source: Vector2,
        target: Vector2,
        verticalBaseImpulse: Float,
        horizontalScalar: Float = 1f,
        verticalScalar: Float = 1f,
        out: Vector2 = GameObjectPools.fetch(Vector2::class)
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
        verticalScalar: Float,
        out: Vector2 = GameObjectPools.fetch(Vector2::class)
    ): Vector2 {
        val horizontalDistance = targetX - sourceX
        val verticalDistance = targetY - sourceY

        val impulseX = horizontalDistance * horizontalScalar
        val impulseY = verticalBaseImpulse + (verticalDistance * verticalScalar)

        return out.set(impulseX, impulseY)
    }
}
