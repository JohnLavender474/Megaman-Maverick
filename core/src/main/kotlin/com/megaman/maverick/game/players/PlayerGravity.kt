package com.megaman.maverick.game.players

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.components.IGameComponent

class PlayerGravityValues(
    val ground: Float,
    val jump: Float,
    val fall: Float,
    val ice: Float,
    val water: Float,
    val waterIce: Float
)

class PlayerGravityComponent(private val values: PlayerGravityValues) : IGameComponent {

    var ground = 0f
        private set
    var jump = 0f
        private set
    var fall = 0f
        private set
    var ice = 0f
        private set
    var water = 0f
        private set
    var waterIce = 0f
        private set

    fun applyDirection(direction: Direction) {
        val sign = when (direction) {
            Direction.UP, Direction.RIGHT -> 1f
            Direction.DOWN, Direction.LEFT -> -1f
        }
        ground = values.ground * sign
        jump = values.jump * sign
        fall = values.fall * sign
        ice = values.ice * sign
        water = values.water * sign
        waterIce = values.waterIce * sign
    }
}
