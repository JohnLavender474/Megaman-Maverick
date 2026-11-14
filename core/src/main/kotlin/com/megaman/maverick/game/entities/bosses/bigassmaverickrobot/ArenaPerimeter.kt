package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstVals

enum class ArenaSide(val outward: Direction) {
    LEFT_WALL(Direction.RIGHT),
    FLOOR(Direction.UP),
    RIGHT_WALL(Direction.LEFT)
}

object ArenaPerimeter {

    const val CORNER_GAP = 0.5f

    fun length(arena: GameRectangle) = 2f * arena.getHeight() + arena.getWidth()

    fun offsetOf(arena: GameRectangle, point: Vector2, side: ArenaSide) = when (side) {
        ArenaSide.LEFT_WALL -> arena.getMaxY() - point.y
        ArenaSide.FLOOR -> arena.getHeight() + point.x - arena.getX()
        ArenaSide.RIGHT_WALL -> arena.getHeight() + arena.getWidth() + point.y - arena.getY()
    }

    fun pointAt(arena: GameRectangle, offset: Float, out: Vector2): ArenaSide {
        val height = arena.getHeight()
        val width = arena.getWidth()
        val gap = CORNER_GAP * ConstVals.PPM
        return when {
            offset < height -> {
                out.set(arena.getX(), MathUtils.clamp(arena.getMaxY() - offset, arena.getY() + gap, arena.getMaxY() - gap))
                ArenaSide.LEFT_WALL
            }

            offset <= height + width -> {
                out.set(MathUtils.clamp(arena.getX() + offset - height, arena.getX() + gap, arena.getMaxX() - gap), arena.getY())
                ArenaSide.FLOOR
            }

            else -> {
                out.set(
                    arena.getMaxX(),
                    MathUtils.clamp(arena.getY() + offset - height - width, arena.getY() + gap, arena.getMaxY() - gap)
                )
                ArenaSide.RIGHT_WALL
            }
        }
    }
}
