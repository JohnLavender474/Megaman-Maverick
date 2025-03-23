package com.mega.game.engine.common.shapes

import kotlin.math.ceil
import kotlin.math.floor

data class MinsAndMaxes(var minX: Int, var minY: Int, var maxX: Int, var maxY: Int) {

    companion object {

        fun of(bounds: GameRectangle): MinsAndMaxes = MinsAndMaxes(
            floor(bounds.getX()).toInt(), floor(bounds.getY()).toInt(),
            ceil(bounds.getMaxX()).toInt(), ceil(bounds.getMaxY()).toInt()
        )
    }

    constructor(): this(0, 0, 0, 0)

    fun set(minX: Int, minY: Int, maxX: Int, maxY: Int): MinsAndMaxes {
        this.minX = minX
        this.minY = minY
        this.maxX = maxX
        this.maxY = maxY
        return this
    }
}
