package com.megaman.maverick.game.players

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.buttons.ButtonActuator
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.entities.state.shouldPreserveMomentum
import com.megaman.maverick.game.entities.state.stopMomentum
import com.megaman.maverick.game.entities.state.teleporting
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs

private const val TAG = "PlayerRun"

class RunParams(
    val impulse: Float,
    val iceImpulse: Float,
    val ramp1Time: Float,
    val ramp1Scalar: Float,
    val ramp2Time: Float,
    val ramp2Scalar: Float,
    val ramp3Scalar: Float,
    val maxSpeed: () -> Float,
    val movementScalar: () -> Float,
    val blocked: () -> Boolean,
    val onBlocked: (runFacing: Facing) -> Unit
)

fun IBodyEntity.playerMaxRunSpeed(maxSpeed: Float, waterScalar: Float): Float {
    var threshold = maxSpeed * ConstVals.PPM
    if (body.isSensing(BodySense.IN_WATER)) threshold *= waterScalar
    return threshold
}

fun <T> T.playerRun(delta: Float, side: Facing, params: RunParams)
    where T : AbstractHealthEntity, T : IBodyEntity, T : IFaceable, T : IDirectional {
    if (!isRunSidePressed(side)) return

    val runState = require(RunComponent::class)

    val lastPressedJustReleased =
        runState.lastPressedSide != side && input.isJustReleased(side.opposite().runButton())
    val runFacing =
        if (isBehaviorActive(BehaviorType.WALL_SLIDING) && !lastPressedJustReleased) side.opposite() else side

    if (!inPlay || !playerCanMove || damaged || teleporting || params.blocked()) {
        running = false
        params.onBlocked(runFacing)
        return
    }

    facing = runFacing

    running = !isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.GROUND_SLIDING, BehaviorType.CLIMBING)
    if (!running) return

    runState.runTime += delta

    val threshold = params.maxSpeed()

    val rawImpulse = if (body.isSensing(BodySense.FEET_ON_ICE)) params.iceImpulse else params.impulse

    var impulse = rawImpulse * delta * params.movementScalar() * ConstVals.PPM * side.value

    impulse *= if (body.isSensing(BodySense.FEET_ON_GROUND)) when {
        runState.runTime < params.ramp1Time -> params.ramp1Scalar
        runState.runTime < params.ramp2Time -> params.ramp2Scalar
        else -> params.ramp3Scalar
    } else 1f

    when {
        direction == Direction.UP && abs(body.physics.velocity.x) < threshold -> body.physics.velocity.x += impulse
        direction == Direction.DOWN && abs(body.physics.velocity.x) < threshold -> body.physics.velocity.x -= impulse
        direction == Direction.LEFT && abs(body.physics.velocity.y) < threshold -> body.physics.velocity.y += impulse
        direction == Direction.RIGHT && abs(body.physics.velocity.y) < threshold -> body.physics.velocity.y -= impulse
    }
}

fun <T> T.playerRunButton(side: Facing, params: RunParams): ButtonActuator
    where T : AbstractHealthEntity, T : IBodyEntity, T : IFaceable, T : IDirectional {
    val otherButton = side.opposite().runButton()

    fun onNotRunning() {
        running = false
    }

    return ButtonActuator(
        onJustPressed = {
            GameLogger.debug(TAG, "$side actuator just pressed")
            require(RunComponent::class).lastPressedSide = side
        },
        onPressContinued = { delta -> playerRun(delta, side, params) },
        onJustReleased = {
            GameLogger.debug(TAG, "$side actuator just released")
            if (!input.isPressed(otherButton)) {
                if (!shouldPreserveMomentum(true)) stopMomentum(true)
                require(RunComponent::class).runTime = 0f
                onNotRunning()
            }
        },
        onReleaseContinued = { if (!input.isPressed(otherButton)) onNotRunning() }
    )
}
