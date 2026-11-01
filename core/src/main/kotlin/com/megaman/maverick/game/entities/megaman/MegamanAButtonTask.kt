package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.components.bodyOverGround
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

internal fun Megaman.updateAButtonTask() {
    if (bodyOverGround) airDashConsumed = false

    if (!bodyOverGround && aButtonTask == AButtonTask.JUMP &&
        !isAnyBehaviorActive(
            BehaviorType.WALL_SLIDING,
            BehaviorType.CLIMBING,
            BehaviorType.JUMPING
        )
    ) aButtonTask = when {
        body.isSensing(BodySense.IN_WATER) -> AButtonTask.SWIM
        !airDashConsumed -> AButtonTask.AIR_DASH
        else -> AButtonTask.JUMP
    }

    if (bodyOverGround && aButtonTask == AButtonTask.AIR_DASH &&
        !body.isSensing(BodySense.IN_WATER) &&
        !isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.AIR_DASHING)
    ) aButtonTask = AButtonTask.JUMP
}
