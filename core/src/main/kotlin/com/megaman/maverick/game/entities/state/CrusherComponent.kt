package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.IGameEntity
import com.megaman.maverick.game.entities.depleteHealth
import com.megaman.maverick.game.entities.feetFixture
import com.megaman.maverick.game.entities.headFixture
import com.megaman.maverick.game.entities.leftSideFixture
import com.megaman.maverick.game.entities.rightSideFixture
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

class CrusherComponent(
    private val bounds: () -> GameRectangle,
    private val side: (IGameEntity) -> Direction,
    private val active: () -> Boolean = { true },
    private val needsSolidOpposite: Boolean = false
) : IGameComponent {

    fun crushes(entity: IGameEntity): Boolean {
        if (!active()) return false

        val side = side(entity)
        val shape = when (side) {
            Direction.UP -> entity.headFixture
            Direction.DOWN -> entity.feetFixture
            Direction.LEFT -> entity.leftSideFixture
            Direction.RIGHT -> entity.rightSideFixture
        }.getShape()

        return bounds().overlaps(shape) && (!needsSolidOpposite || entity.body.isSensing(oppositeSense(side)))
    }

    fun crush(entity: IGameEntity) {
        if (crushes(entity)) entity.depleteHealth()
    }

    private fun oppositeSense(side: Direction) = when (side) {
        Direction.UP -> BodySense.FEET_ON_GROUND
        Direction.DOWN -> BodySense.HEAD_TOUCHING_BLOCK
        Direction.LEFT -> BodySense.SIDE_TOUCHING_BLOCK_RIGHT
        Direction.RIGHT -> BodySense.SIDE_TOUCHING_BLOCK_LEFT
    }
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
