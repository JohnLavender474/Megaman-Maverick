package com.mega.game.engine.common.enums

import com.mega.game.engine.common.enums.Position.entries


enum class Position(val x: Int, val y: Int) {
    BOTTOM_LEFT(0, 0),
    BOTTOM_CENTER(1, 0),
    BOTTOM_RIGHT(2, 0),
    CENTER_LEFT(0, 1),
    CENTER(1, 1),
    CENTER_RIGHT(2, 1),
    TOP_LEFT(0, 2),
    TOP_CENTER(1, 2),
    TOP_RIGHT(2, 2);

    companion object {

        fun get(x: Int, y: Int): Position {
            if (x < 0 || x > 2 || y < 0 || y > 2)
                throw IndexOutOfBoundsException("No position value for x=$x and y=$y")

            val index = x + y * 3
            return entries[index]
        }
    }

    fun move(direction: Direction) = when (direction) {
        Direction.UP -> up()
        Direction.LEFT -> left()
        Direction.DOWN -> down()
        Direction.RIGHT -> right()
    }

    fun left(): Position {
        var nextX = x - 1
        if (nextX < 0) nextX = 2
        return get(nextX, y)
    }

    fun right(): Position {
        var nextX = x + 1
        if (nextX > 2) nextX = 0
        return get(nextX, y)
    }

    fun up(): Position {
        var nextY = y + 1
        if (nextY > 2) nextY = 0
        return get(x, nextY)
    }

    fun down(): Position {
        var nextY = y - 1
        if (nextY < 0) nextY = 2
        return get(x, nextY)
    }

    fun opposite(): Position {
        val newX = when (x) {
            0 -> 2
            1 -> 1
            else -> 0
        }
        val newY = when (y) {
            0 -> 2
            1 -> 1
            else -> 0
        }
        return get(newX, newY)
    }
}
