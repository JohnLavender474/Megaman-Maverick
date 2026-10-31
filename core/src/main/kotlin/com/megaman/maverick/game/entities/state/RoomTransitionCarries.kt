package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity

val IGameEntity.roomTransitionPaused: Boolean
    get() = getComponent(RoomTransitionCarryComponent::class)?.pauseTimer?.isFinished() == false

fun IBodyEntity.beginRoomTransitionCarry() {
    val carry = require(RoomTransitionCarryComponent::class)
    carry.pauseTimer.reset()
    carry.clearCarriedValues()

    if (!carry.carries()) body.physics.velocity.setZero()
    else when (body.direction) {
        Direction.UP, Direction.DOWN -> carry.carriedY = body.physics.velocity.y
        else -> carry.carriedX = body.physics.velocity.x
    }
}

fun IBodyEntity.endRoomTransitionCarry() {
    val carry = require(RoomTransitionCarryComponent::class)

    if (!carry.carries()) body.physics.velocity.setZero()
    else {
        carry.carriedX?.let { body.physics.velocity.x = it }
        carry.carriedY?.let { body.physics.velocity.y = it }
    }

    carry.clearCarriedValues()
}

private fun RoomTransitionCarryComponent.clearCarriedValues() {
    carriedX = null
    carriedY = null
}
