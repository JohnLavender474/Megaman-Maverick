package com.megaman.maverick.game.entities.megaman.sprites

import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.feetOnGround
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

fun Megaman.getAnimationKey(priorAnimKey: String?) = when {
    !roomTransPauseTimer.isFinished() -> null

    priorAnimKey != null && game.isProperty(ConstKeys.ROOM_TRANSITION, true) -> {
        var key = priorAnimKey
        if (key.contains("stand")) key = key.replace("stand", "run")
        key
    }

    game.isCameraRotating() -> amendKey("jump")

    !ready -> "spawn"

    stunned -> "stunned"

    damaged -> if (isBehaviorActive(BehaviorType.RIDING_CART)) "cartin_damaged" else "damaged"

    isBehaviorActive(BehaviorType.JETPACKING) -> amendKey("jetpack")

    isBehaviorActive(BehaviorType.RIDING_CART) -> when {
        isBehaviorActive(BehaviorType.JUMPING) || !feetOnGround -> amendKey("cartin_jump")
        else -> amendKey("cartin")
    }

    isBehaviorActive(BehaviorType.CLIMBING) -> when {
        !body.isSensing(BodySense.HEAD_TOUCHING_LADDER) && !shooting -> amendKey("climb_finish")

        else -> {
            val movement = if (direction.isHorizontal()) body.physics.velocity.x else body.physics.velocity.y
            val key = if (movement != 0f) "climb" else "climb_still"
            amendKey(key)
        }
    }

    isBehaviorActive(BehaviorType.AIR_DASHING) -> amendKey("airdash")

    isBehaviorActive(BehaviorType.CROUCHING) -> amendKey("crouch")

    isBehaviorActive(BehaviorType.GROUND_SLIDING) -> amendKey("groundslide")

    isBehaviorActive(BehaviorType.WALL_SLIDING) -> amendKey("wallslide")

    isBehaviorActive(BehaviorType.SWIMMING) -> amendKey("swim")

    isBehaviorActive(BehaviorType.JUMPING) || !feetOnGround -> amendKey("jump")

    running -> amendKey("run")

    slipSliding -> amendKey("slip")

    else -> amendKey("stand")
}

fun Megaman.amendKey(baseKey: String) = when {
    shooting -> "${baseKey}_shoot"
    fullyCharged -> "${baseKey}_charge_full"
    halfCharged -> "${baseKey}_charge_half"
    else -> baseKey

}

