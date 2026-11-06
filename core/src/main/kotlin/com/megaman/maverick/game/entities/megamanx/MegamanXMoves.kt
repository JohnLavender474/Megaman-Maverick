package com.megaman.maverick.game.entities.megamanx

import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.players.AirDashParams
import com.megaman.maverick.game.players.JumpParams
import com.megaman.maverick.game.players.WallSlideParams
import com.megaman.maverick.game.players.airDashInputJustPressed
import com.megaman.maverick.game.players.bodyOverGround
import com.megaman.maverick.game.players.isRunSidePressed
import com.megaman.maverick.game.players.playerAirDashBehavior
import com.megaman.maverick.game.players.playerJumpBehavior
import com.megaman.maverick.game.players.playerWallSlideBehavior
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.entities.state.stunned
import com.megaman.maverick.game.players.playerCanMove

// TODO: the wall grab, the wall kick and the air dash play no sound yet (owner decision D4 of 081c)
internal fun MegamanX.defineBehaviorsComponent(): BehaviorsComponent {
    val component = BehaviorsComponent()
    component.addBehavior(BehaviorType.WALL_SLIDING, wallSlideBehavior())
    component.addBehavior(BehaviorType.JUMPING, jumpBehavior())
    component.addBehavior(BehaviorType.AIR_DASHING, airDashBehavior())
    return component
}

internal val MegamanX.kickLocked: Boolean
    get() = !kickLockTimer.isFinished()

internal val MegamanX.pushingOff: Boolean
    get() = !pushOffTimer.isFinished()

internal val MegamanX.holdsSideSpeed: Boolean
    get() = kickLocked || pushingOff || isBehaviorActive(BehaviorType.AIR_DASHING)

internal fun MegamanX.touchedWallSide(): Facing? = when {
    body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) -> Facing.LEFT
    body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) -> Facing.RIGHT
    else -> null
}

internal fun MegamanX.setUpSpeed(speed: Float) = when (direction) {
    Direction.UP -> body.physics.velocity.y = speed
    Direction.DOWN -> body.physics.velocity.y = -speed
    Direction.LEFT -> body.physics.velocity.x = -speed
    Direction.RIGHT -> body.physics.velocity.x = speed
}

internal fun MegamanX.setSideSpeed(speed: Float) = when (direction) {
    Direction.UP -> body.physics.velocity.x = speed
    Direction.DOWN -> body.physics.velocity.x = -speed
    Direction.LEFT -> body.physics.velocity.y = speed
    Direction.RIGHT -> body.physics.velocity.y = -speed
}

private fun MegamanX.wallSlideBehavior() = playerWallSlideBehavior(
    WallSlideParams(
        blocked = { kickLocked },
        onStart = {
            wallGrabTimer.reset()
            airDashUsed = false
        },
        onAct = { delta ->
            wallGrabTimer.update(delta)
            body.physics.gravityOn = false
            body.physics.velocity.setZero()
            if (wallGrabTimer.isFinished()) setUpSpeed(-MegamanXValues.WALL_SLIDE_SPEED * ConstVals.PPM)
        },
        onEnd = {
            val wall = touchedWallSide()
            if (wall != null && !isRunSidePressed(wall) && !isBehaviorActive(BehaviorType.JUMPING) && !bodyOverGround &&
                playerCanMove && !damaged && !stunned
            ) {
                pushOffTimer.reset()
                setSideSpeed(MegamanXValues.PUSH_OFF_SPEED * ConstVals.PPM * facing.value)
            }
        }
    )
)

private fun MegamanX.jumpBehavior() = playerJumpBehavior(
    JumpParams(
        canStart = { true },
        blocked = { isBehaviorActive(BehaviorType.AIR_DASHING) },
        jumpSpeed = { upSign() * MegamanXValues.JUMP_SPEED },
        wallJumpSpeed = { upSign() * MegamanXValues.WALL_KICK_SPEED },
        wallJumpHorizontal = 0f,
        wallJumpAllowed = {
            isBehaviorActive(BehaviorType.WALL_SLIDING) ||
                (!bodyOverGround && touchedWallSide() != null && wallDelayTimer.isFinished())
        },
        ignoreRelease = { kickLocked },
        onJump = {
            wallDelayTimer.reset()
            val wall = touchedWallSide()
            if (!bodyOverGround && wall != null) {
                facing = wall
                setSideSpeed(MegamanXValues.WALL_KICK_AWAY_SPEED * ConstVals.PPM * wall.opposite().value)
                kickLockTimer.reset()
                pushOffTimer.setToEnd()
            }
        }
    )
)

private fun MegamanX.airDashBehavior() = playerAirDashBehavior(
    AirDashParams(
        blocked = {
            bodyOverGround || (isBehaviorActive(BehaviorType.AIR_DASHING) && isRunSidePressed(facing.opposite()))
        },
        canStart = {
            val jumpTookA = isBehaviorActive(BehaviorType.JUMPING) && input.isJustPressed(MegaControllerButton.A)
            // TODO: the dash button gives the same air dash as A; its faster X6 start speed comes in phase 3 of item 081
            !airDashUsed && !kickLocked &&
                (input.isJustPressed(MegaControllerButton.R) || (airDashInputJustPressed() && !jumpTookA))
        },
        held = { false },
        startDelay = MegamanXValues.AIR_DASH_HOVER_TIME,
        minTime = MegamanXValues.AIR_DASH_HOVER_TIME + MegamanXValues.AIR_DASH_TIME,
        maxTime = { MegamanXValues.AIR_DASH_HOVER_TIME + MegamanXValues.AIR_DASH_TIME },
        speed = { MegamanXValues.AIR_DASH_SPEED },
        endBump = { 0f },
        onStart = {
            airDashUsed = true
            when {
                isRunSidePressed(Facing.LEFT) -> facing = Facing.LEFT
                isRunSidePressed(Facing.RIGHT) -> facing = Facing.RIGHT
            }
        },
        onEnd = {}
    )
)
