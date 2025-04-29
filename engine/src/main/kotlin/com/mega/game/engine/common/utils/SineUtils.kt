package com.mega.game.engine.common.utils

import com.badlogic.gdx.math.MathUtils

object SineUtils {

    fun y(amplitude: Float, frequency: Float, elapsedTime: Float) =
        amplitude * MathUtils.sin(frequency * elapsedTime)

    fun yBetween0and1(amplitude: Float, frequency: Float, elapsedTime: Float): Float {
        val sinValue = MathUtils.sin(frequency * elapsedTime)
        return amplitude * (sinValue + 1) / 2
    }
}
