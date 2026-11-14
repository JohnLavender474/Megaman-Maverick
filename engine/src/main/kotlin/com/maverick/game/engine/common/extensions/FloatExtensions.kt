package com.maverick.game.engine.common.extensions

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

fun Float.round(decimals: Int = 2) = BigDecimal(toDouble()).setScale(decimals, RoundingMode.HALF_UP).toFloat()

fun Float.coerceIn(abs: Float) = coerceIn(-abs(abs), abs(abs))

fun Float.epsilonEquals(other: Float, epsilon: Float) = abs(other - this) <= epsilon

fun Float.isBetween(i: Float, j: Float) = this in i..j

fun Float.lessThan(other: Float) = this < other
