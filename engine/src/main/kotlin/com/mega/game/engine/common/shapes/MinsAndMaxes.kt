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
}