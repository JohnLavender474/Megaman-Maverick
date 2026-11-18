package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle

object GravityFrame {

    fun toWorld(direction: Direction, local: GameRectangle, out: GameRectangle): GameRectangle {
        val x = local.getX()
        val y = local.getY()
        val width = local.getWidth()
        val height = local.getHeight()
        return when (direction) {
            Direction.UP -> out.set(x, y, width, height)
            Direction.DOWN -> out.set(x, -(y + height), width, height)
            Direction.LEFT -> out.set(-(y + height), x, height, width)
            Direction.RIGHT -> out.set(y, x, height, width)
        }
    }

    fun toLocal(direction: Direction, world: GameRectangle, out: GameRectangle): GameRectangle {
        val x = world.getX()
        val y = world.getY()
        val width = world.getWidth()
        val height = world.getHeight()
        return when (direction) {
            Direction.UP -> out.set(x, y, width, height)
            Direction.DOWN -> out.set(x, -(y + height), width, height)
            Direction.LEFT -> out.set(y, -(x + width), height, width)
            Direction.RIGHT -> out.set(y, x, height, width)
        }
    }

    fun velocityToLocal(direction: Direction, world: Vector2, out: Vector2): Vector2 =
        when (direction) {
            Direction.UP -> out.set(world.x, world.y)
            Direction.DOWN -> out.set(world.x, -world.y)
            Direction.LEFT -> out.set(world.y, -world.x)
            Direction.RIGHT -> out.set(world.y, world.x)
        }

    fun velocityToWorld(direction: Direction, localX: Float, localY: Float, out: Vector2): Vector2 =
        when (direction) {
            Direction.UP -> out.set(localX, localY)
            Direction.DOWN -> out.set(localX, -localY)
            Direction.LEFT -> out.set(-localY, localX)
            Direction.RIGHT -> out.set(localY, localX)
        }
}
