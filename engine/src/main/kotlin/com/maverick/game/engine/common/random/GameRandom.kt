package com.maverick.game.engine.common.random

import com.badlogic.gdx.utils.Array

interface GameRandom {

    fun nextInt(min: Int, maxInclusive: Int): Int

    fun nextFloat(min: Float, max: Float): Float

    fun nextBoolean(): Boolean

    fun <T> pick(array: Array<T>): T

    fun <T> pick(list: List<T>): T

    fun <T> pickOf(vararg values: T): T

    fun <T> shuffle(array: Array<T>)
}
