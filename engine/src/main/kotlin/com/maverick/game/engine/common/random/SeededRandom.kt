package com.maverick.game.engine.common.random

import com.badlogic.gdx.utils.Array
import java.util.Random

class SeededRandom(private val source: Random) : GameRandom {

    constructor(seed: Long) : this(Random(seed))

    override fun nextInt(min: Int, maxInclusive: Int): Int {
        require(maxInclusive >= min) { "maxInclusive ($maxInclusive) must not be less than min ($min)" }
        return source.nextInt(maxInclusive + 1 - min) + min
    }

    override fun nextFloat(min: Float, max: Float) = source.nextFloat() * (max - min) + min

    override fun nextBoolean() = source.nextBoolean()

    override fun <T> pick(array: Array<T>): T {
        require(!array.isEmpty) { "Cannot pick from an empty array" }
        return array[source.nextInt(array.size)]
    }

    override fun <T> pick(list: List<T>): T {
        require(list.isNotEmpty()) { "Cannot pick from an empty list" }
        return list[source.nextInt(list.size)]
    }

    override fun <T> pickOf(vararg values: T): T {
        require(values.isNotEmpty()) { "Cannot pick from no values" }
        return values[source.nextInt(values.size)]
    }

    override fun <T> shuffle(array: Array<T>) {
        for (i in array.size - 1 downTo 1) array.swap(i, source.nextInt(i + 1))
    }
}
