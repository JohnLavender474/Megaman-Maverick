package com.mega.game.engine.common.enums

import com.mega.game.engine.common.enums.Direction.values


enum class Direction(val rotation: Float) {

    UP(0f), LEFT(90f), DOWN(180f), RIGHT(270f);


    fun getOpposite() = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }


    fun isHorizontal() = this == LEFT || this == RIGHT


    fun isVertical() = this == UP || this == DOWN


    fun getRotatedClockwise(): Direction {
        var index = ordinal - 1
        if (index < 0) index = values().size - 1
        return values()[index]
    }


    fun getRotatedCounterClockwise(): Direction {
        var index = ordinal + 1
        if (index == values().size) index = 0
        return values()[index]
    }
}
