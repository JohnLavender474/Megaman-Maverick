package com.megaman.maverick.game.entities.megaman.components

import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.state.PostActionMomentumComponent
import com.megaman.maverick.game.entities.state.RoomTransitionCarryComponent
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.players.bodyOverGround

internal fun Megaman.defineMomentumComponent() = PostActionMomentumComponent(
    MegamanValues.POST_ACTION_MOMENTUM_DUR,
    MegamanValues.POST_ACTION_BURST_RESISTANCE,
    maxSpeed = { getMaxRunSpeed() },
    overGround = { bodyOverGround },
    facingBlock = { FacingUtils.isFacingBlock(this) },
    groundSliding = { isBehaviorActive(BehaviorType.GROUND_SLIDING) }
)

internal fun Megaman.defineRoomTransitionCarryComponent() = RoomTransitionCarryComponent(
    ConstVals.ROOM_TRANS_DELAY_DURATION,
    carries = { !isBehaviorActive(BehaviorType.CLIMBING) && bodyOverGround }
)
