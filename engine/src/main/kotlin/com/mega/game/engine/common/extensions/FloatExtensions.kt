package com.mega.game.engine.common.extensions

import kotlin.math.abs

fun Float.round(decimals: Int = 2) = "%.${decimals}f".format(this).toFloat()

fun Float.epsilonEquals(other: Float, epsilon: Float) = abs(other - this) <= epsilon
