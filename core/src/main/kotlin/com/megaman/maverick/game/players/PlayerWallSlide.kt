package com.megaman.maverick.game.players

import com.maverick.game.engine.behaviors.FunctionalBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

private const val TAG = "PlayerWallSlide"

class WallSlideParams(
    val blocked: () -> Boolean,
    val allowedWhileJumping: () -> Boolean,
    val holdsWithoutInput: () -> Boolean,
    val onStart: () -> Unit,
    val onAct: (Float) -> Unit,
    val onEnd: () -> Unit
)

private fun pressedWallSide(player: MegaPlayer): Facing? = when {
    player.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && player.isRunSidePressed(Facing.LEFT) -> Facing.LEFT
    player.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && player.isRunSidePressed(Facing.RIGHT) -> Facing.RIGHT
    else -> null
}

// TODO: widen the player type when an enemy clone calls this (item 121)
fun playerWallSlideBehavior(player: MegaPlayer, params: WallSlideParams): IBehavior = FunctionalBehaviorImpl(
    evaluate = {
        !(!player.existenceState.liveAfterUpdate || !player.inPlay || !player.playerCanMove || params.blocked()) &&
            (pressedWallSide(player) != null ||
                (player.isBehaviorActive(BehaviorType.WALL_SLIDING) && params.holdsWithoutInput())) && !player.damaged &&
            (!player.isBehaviorActive(BehaviorType.JUMPING) || params.allowedWhileJumping()) &&
            !player.isBehaviorActive(BehaviorType.CLIMBING) &&
            !player.body.isSensing(BodySense.FEET_ON_GROUND)
    },
    init = {
        params.onStart()
        player.facing = pressedWallSide(player)!!.opposite()
        GameLogger.debug(TAG, "init()")
    },
    act = { delta -> params.onAct(delta) },
    end = {
        params.onEnd()
        player.body.physics.gravityOn = true
        GameLogger.debug(TAG, "end()")
    }
)
