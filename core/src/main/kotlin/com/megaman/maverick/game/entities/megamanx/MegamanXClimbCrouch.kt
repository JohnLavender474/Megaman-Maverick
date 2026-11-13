package com.megaman.maverick.game.entities.megamanx

import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.locks.unlock
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.entities.state.teleporting
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.players.ClimbEntry
import com.megaman.maverick.game.players.ClimbParams
import com.megaman.maverick.game.players.bodyOverGround
import com.megaman.maverick.game.players.playerCanMove
import com.megaman.maverick.game.players.playerClimbBehavior
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

internal enum class LadderPose { CLIMB_DOWN, CLIMB_OVER }

internal fun MegamanX.startLadderPose(pose: LadderPose) {
    ladderPose = pose
    when (pose) {
        LadderPose.CLIMB_DOWN -> ladderPoseTimer.resetDuration(MegamanXValues.CLIMB_START_TIME)
        LadderPose.CLIMB_OVER -> {
            ladderPoseTimer.resetDuration(MegamanXValues.CLIMB_FINISH_TIME)
            lock(MegaLockGroup.MOVEMENT, MegaLockReason.LADDER)
        }
    }
}

internal fun MegamanX.endLadderPose() {
    if (ladderPose == LadderPose.CLIMB_OVER) unlock(MegaLockGroup.MOVEMENT, MegaLockReason.LADDER)
    ladderPose = null
    ladderPoseTimer.setToEnd()
}

internal fun MegamanX.updateLadderPose(delta: Float) {
    if (ladderPose == null) return
    ladderPoseTimer.update(delta)
    if (ladderPoseTimer.isFinished()) endLadderPose()
}

internal fun MegamanX.onLadderTop() =
    bodyOverGround && body.isSensing(BodySense.FEET_TOUCHING_LADDER) && !body.isSensing(BodySense.HEAD_TOUCHING_LADDER)

internal fun MegamanX.climbBehavior(): IBehavior = playerClimbBehavior(
    this,
    ClimbParams(
        speed = { MegamanXValues.CLIMB_SPEED },
        blocked = { isAnyBehaviorActive(BehaviorType.GROUND_DASHING) || sabre.active },
        feetGrabAllowed = { onLadderTop() },
        holdStill = { ladderPose == LadderPose.CLIMB_DOWN },
        leavesOnFloor = true,
        onStart = { entry ->
            airDashUsed = false
            dashSpeedInAir = false
            running = false
            if (entry == ClimbEntry.FEET) startLadderPose(LadderPose.CLIMB_DOWN)
        },
        onEnd = { overTop ->
            endLadderPose()
            if (overTop) startLadderPose(LadderPose.CLIMB_OVER)
        }
    )
)

internal class Crouch(private val x: MegamanX) : AbstractBehaviorImpl() {

    private val riseTimer = Timer(MegamanXValues.CROUCH_FRAME_TIME).setToEnd()

    var rising = false
        private set

    private fun downHeld() = x.input.isPressed(MegaControllerButton.DOWN)

    override fun evaluate(delta: Float): Boolean {
        // TODO: X does not crouch with a sideways gravity; no X level turns the gravity sideways yet (item 081)
        if (!x.existenceState.liveAfterUpdate || !x.inPlay || !x.playerCanMove || x.damaged || x.teleporting ||
            !x.bodyOverGround || x.direction.isHorizontal() || x.sabre.active || x.services.gameCamera.rotating ||
            x.isAnyBehaviorActive(
                BehaviorType.JUMPING,
                BehaviorType.GROUND_DASHING,
                BehaviorType.AIR_DASHING,
                BehaviorType.WALL_SLIDING,
                BehaviorType.CLIMBING
            )
        ) return false

        if (isActive()) {
            if (rising) {
                riseTimer.update(delta)
                return !riseTimer.isFinished()
            }
            if (!downHeld()) {
                rising = true
                riseTimer.reset()
            }
            return true
        }

        return downHeld() && !x.onLadderTop()
    }

    override fun init(vararg params: Any) {
        rising = false
        x.running = false
        x.setSideSpeed(0f)
    }

    override fun act(delta: Float) = Unit

    override fun end() {
        rising = false
    }
}

internal fun MegamanX.crouchBehavior(): IBehavior = Crouch(this).also { crouch = it }
