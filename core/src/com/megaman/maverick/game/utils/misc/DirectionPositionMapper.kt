package com.megaman.maverick.game.utils.misc

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.objectMapOf

object DirectionPositionMapper {

    private val directionToPosition = objectMapOf(
        Direction.UP to Position.TOP_CENTER,
        Direction.DOWN to Position.BOTTOM_CENTER,
        Direction.LEFT to Position.CENTER_LEFT,
        Direction.RIGHT to Position.CENTER_RIGHT
    )
    private val invertedDirectionToPosition = objectMapOf(
        Direction.UP to Position.BOTTOM_CENTER,
        Direction.DOWN to Position.TOP_CENTER,
        Direction.LEFT to Position.CENTER_RIGHT,
        Direction.RIGHT to Position.CENTER_LEFT
    )
    private val positionToDirection = ObjectMap<Position, Direction>()

    init {
        directionToPosition.forEach { entry ->
            positionToDirection.put(entry.value, entry.key)
        }
    }

    fun getPosition(direction: Direction): Position {
        if (!directionToPosition.containsKey(direction)) throw IllegalArgumentException("Illegal parameter: $direction")
        return directionToPosition[direction]
    }

    fun getInvertedPosition(direction: Direction): Position {
        if (!invertedDirectionToPosition.containsKey(direction)) throw IllegalArgumentException("Illegal parameter: $direction")
        return invertedDirectionToPosition[direction]
    }

    fun getDirection(position: Position): Direction {
        if (!positionToDirection.containsKey(position)) throw IllegalArgumentException("Illegal parameter: $position")
        return positionToDirection[position]
    }
}