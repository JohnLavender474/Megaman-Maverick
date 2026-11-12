package com.megaman.maverick.game.entities.bosses.d1000

import kotlin.math.max
import kotlin.math.min

internal const val FLICKER_STEP = 0.05f

internal fun flickerOn(time: Float, step: Float = FLICKER_STEP) = (time / step).toInt() % 2 == 1

internal fun approach(current: Float, target: Float, speed: Float, delta: Float): Float {
    val step = speed * delta
    return if (current < target) min(current + step, target) else max(current - step, target)
}

internal interface D1000Move {

    val holdsEyeShots: Boolean

    fun begin()

    fun update(delta: Float)

    fun forecastNow()

    fun forecast(startIn: Float)

    fun allowed(startIn: Float): Boolean
}
