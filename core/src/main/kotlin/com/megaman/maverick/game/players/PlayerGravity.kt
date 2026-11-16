package com.megaman.maverick.game.players

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.locks.lock
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.utils.extensions.startSmoothFollow

fun Direction.upSign() = when (this) {
    Direction.UP, Direction.RIGHT -> 1f
    Direction.DOWN, Direction.LEFT -> -1f
}

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
        val sign = direction.upSign()
        ground = values.ground * sign
        jump = values.jump * sign
        fall = values.fall * sign
        ice = values.ice * sign
        water = values.water * sign
        waterIce = values.waterIce * sign
    }
}

// TODO: the camera turn belongs to WorldGravity, not to each player; with more players, each player's setter asks for
//  the turn (item 058)
fun MegaPlayer.turnBodyAndCamera(value: Direction, onCameraTurnStart: () -> Unit) {
    if (value == body.direction) return
    body.direction = value
    services.cameraController.startSmoothFollow()
    val cameraDirection = if (value.isVertical()) value else value.getOpposite()
    if (services.gameCamera.direction == cameraDirection) return
    services.events.submitEvent(
        Event(EventType.START_GAME_CAM_ROTATION, props(ConstKeys.DIRECTION pairTo cameraDirection))
    )
    lock(MegaLockGroup.MOVEMENT, MegaLockReason.CAMERA_ROTATION)
    onCameraTurnStart()
    body.physics.gravityOn = false
    body.physics.velocity.setZero()
}
