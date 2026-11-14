package com.megaman.maverick.game.players

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.buttons.ButtonActuator
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.movement.RunParams
import com.megaman.maverick.game.entities.movement.runStep
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.entities.state.shouldPreserveMomentum
import com.megaman.maverick.game.entities.state.stopMomentum
import com.megaman.maverick.game.entities.state.teleporting

private const val TAG = "PlayerRun"

private fun playerRun(
    player: MegaPlayer,
    delta: Float,
    side: Facing,
    params: RunParams,
    blocked: () -> Boolean,
    onBlocked: (runFacing: Facing) -> Unit
) {
    if (!player.isRunSidePressed(side)) return

    val runState = player.require(RunComponent::class)

    val lastPressedJustReleased =
        runState.lastPressedSide != side && player.input.isJustReleased(side.opposite().runButton())
    val runFacing =
        if (player.isBehaviorActive(BehaviorType.WALL_SLIDING) && !lastPressedJustReleased) side.opposite() else side

    if (!player.inPlay || !player.playerCanMove || player.damaged || player.teleporting || blocked()) {
        player.running = false
        onBlocked(runFacing)
        return
    }

    player.facing = runFacing

    player.running =
        !player.isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.GROUND_SLIDING, BehaviorType.CLIMBING)
    if (!player.running) return

    runStep(player.body, player.direction, runState, side, delta, params)
}

// TODO: widen the player type when an enemy clone calls this (item 121)
fun playerRunButton(
    player: MegaPlayer,
    side: Facing,
    params: RunParams,
    blocked: () -> Boolean,
    onBlocked: (runFacing: Facing) -> Unit
): ButtonActuator {
    val otherButton = side.opposite().runButton()

    fun onNotRunning() {
        player.running = false
    }

    return ButtonActuator(
        onJustPressed = {
            GameLogger.debug(TAG, "$side actuator just pressed")
            player.require(RunComponent::class).lastPressedSide = side
        },
        onPressContinued = { delta -> playerRun(player, delta, side, params, blocked, onBlocked) },
        onJustReleased = {
            GameLogger.debug(TAG, "$side actuator just released")
            if (!player.input.isPressed(otherButton)) {
                if (!player.shouldPreserveMomentum(true)) player.stopMomentum(true)
                player.require(RunComponent::class).runTime = 0f
                onNotRunning()
            }
        },
        onReleaseContinued = { if (!player.input.isPressed(otherButton)) onNotRunning() }
    )
}

fun MegaPlayer.setSideSpeed(speed: Float) = when (direction) {
    Direction.UP -> body.physics.velocity.x = speed
    Direction.DOWN -> body.physics.velocity.x = -speed
    Direction.LEFT -> body.physics.velocity.y = speed
    Direction.RIGHT -> body.physics.velocity.y = -speed
}
