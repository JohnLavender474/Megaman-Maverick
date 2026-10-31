package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.entities.IGameEntity
import com.megaman.maverick.game.entities.depleteHealth
import com.megaman.maverick.game.entities.feetFixture
import com.megaman.maverick.game.entities.headFixture
import com.megaman.maverick.game.entities.leftSideFixture
import com.megaman.maverick.game.entities.rightSideFixture

class CrusherComponent(
    private val bounds: () -> GameRectangle,
    private val side: (IGameEntity) -> Direction,
    private val active: () -> Boolean = { true },
    private val oppositeTouches: ((IGameShape2D) -> Boolean)? = null
) : IGameComponent {

    fun crushes(entity: IGameEntity): Boolean {
        if (!active()) return false

        val side = side(entity)

        return bounds().overlaps(sideShape(entity, side)) &&
            oppositeTouches?.invoke(sideShape(entity, side.getOpposite())) != false
    }

    fun crush(entity: IGameEntity) {
        if (crushes(entity)) entity.depleteHealth()
    }

    private fun sideShape(entity: IGameEntity, side: Direction) = when (side) {
        Direction.UP -> entity.headFixture
        Direction.DOWN -> entity.feetFixture
        Direction.LEFT -> entity.leftSideFixture
        Direction.RIGHT -> entity.rightSideFixture
    }.getShape()
}

fun worldToBodySide(worldSide: Direction, bodyDirection: Direction): Direction {
    var side = worldSide
    var turns = (bodyDirection.rotation / 90f).toInt()
    while (turns > 0) {
        side = side.getRotatedClockwise()
        turns--
    }
    return side
}
