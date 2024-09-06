package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.mega.game.engine.common.enums.Direction

interface IDirectionRotatable {

    var directionRotation: Direction?

    fun isDirectionRotatedVertically() = isDirectionRotatedUp() || isDirectionRotatedDown()

    fun isDirectionRotatedHorizontally() = isDirectionRotatedLeft() || isDirectionRotatedRight()

    fun isDirectionRotated(direction: Direction): Boolean = directionRotation == direction

    fun isDirectionRotatedDown(): Boolean = isDirectionRotated(Direction.DOWN)

    fun isDirectionRotatedLeft(): Boolean = isDirectionRotated(Direction.LEFT)

    fun isDirectionRotatedRight(): Boolean = isDirectionRotated(Direction.RIGHT)

    fun isDirectionRotatedUp(): Boolean = isDirectionRotated(Direction.UP)

    fun rotateLeft() {
        directionRotation =
            when (directionRotation!!) {
                Direction.UP -> Direction.LEFT
                Direction.LEFT -> Direction.DOWN
                Direction.DOWN -> Direction.RIGHT
                Direction.RIGHT -> Direction.UP
            }
    }

    fun rotateRight() {
        directionRotation =
            when (directionRotation!!) {
                Direction.UP -> Direction.RIGHT
                Direction.RIGHT -> Direction.DOWN
                Direction.DOWN -> Direction.LEFT
                Direction.LEFT -> Direction.UP
            }
    }

    fun rotate180() {
        directionRotation =
            when (directionRotation!!) {
                Direction.UP -> Direction.DOWN
                Direction.RIGHT -> Direction.LEFT
                Direction.DOWN -> Direction.UP
                Direction.LEFT -> Direction.RIGHT
            }
    }
}
