package com.megaman.maverick.game.players

import com.maverick.game.engine.behaviors.FunctionalBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

private const val TAG = "PlayerWallSlide"

class WallSlideParams(
    val blocked: () -> Boolean,
    val onStart: () -> Unit,
    val onAct: (Float) -> Unit,
    val onEnd: () -> Unit
)

private fun <T> T.pressedWallSide(): Facing? where T : IBodyEntity = when {
    body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && isRunSidePressed(Facing.LEFT) -> Facing.LEFT
    body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && isRunSidePressed(Facing.RIGHT) -> Facing.RIGHT
    else -> null
}

fun <T> T.playerWallSlideBehavior(params: WallSlideParams): IBehavior
    where T : AbstractHealthEntity, T : IBodyEntity, T : IFaceable, T : IDirectional = FunctionalBehaviorImpl(
    evaluate = {
        !(!existenceState.liveAfterUpdate || !inPlay || !playerCanMove || params.blocked()) &&
            pressedWallSide() != null && !damaged &&
            !isBehaviorActive(BehaviorType.JUMPING) && !isBehaviorActive(BehaviorType.CLIMBING) &&
            !body.isSensing(BodySense.FEET_ON_GROUND)
    },
    init = {
        params.onStart()
        facing = pressedWallSide()!!.opposite()
        GameLogger.debug(TAG, "init()")
    },
    act = { delta -> params.onAct(delta) },
    end = {
        params.onEnd()
        body.physics.gravityOn = true
        GameLogger.debug(TAG, "end()")
    }
)
