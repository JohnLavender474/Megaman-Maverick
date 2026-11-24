package com.maverick.game.engine.common.extensions

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToInt

fun Float.round(decimals: Int = 2) = BigDecimal(toDouble()).setScale(decimals, RoundingMode.HALF_UP).toFloat()

fun Float.coerceIn(abs: Float) = coerceIn(-abs(abs), abs(abs))

fun Float.epsilonEquals(other: Float, epsilon: Float) = abs(other - this) <= epsilon

fun Float.isBetween(i: Float, j: Float) = this in i..j

fun Float.lessThan(other: Float) = this < other

fun Float.roundAngleToStep(step: Float): Float {
    val stepsInCircle = 360f / step
    require(step > 0f && abs(stepsInCircle - stepsInCircle.roundToInt()) <= ANGLE_STEP_EPSILON) {
        "The angle step $step is not valid: it must be greater than 0 and divide 360 (for example 22.5, 45 or 90)."
    }
    val wrapped = ((this % 360f) + 360f) % 360f
    val rounded = (wrapped / step).roundToInt() * step
    return if (rounded >= 360f) rounded - 360f else rounded
}

private const val ANGLE_STEP_EPSILON = 0.0001f
