package com.megaman.maverick.game.entities.megaman.sprites

import com.mega.game.engine.common.GameLogger
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.feetOnGround
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.special.Ladder
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

const val MEGAMAN_ANIM_KEY_SUPPLIER_TAG = "MEGA_MAN_ANIM_KEY_SUPPLIER"

fun Megaman.getAnimationKey(priorAnimKey: String?) = when {
    !roomTransPauseTimer.isFinished() -> null

    priorAnimKey != null && game.isProperty(ConstKeys.ROOM_TRANSITION, true) -> {
        var key = priorAnimKey
        if (key.contains("stand")) key = key.replace("stand", "run")
        if (key.contains("slip")) key = key.replace("slip", "run")
        key
    }

    game.isCameraRotating() -> amendKey(if (damaged) "damaged" else "jump")

    !ready -> "spawn"

    frozen -> "frozen"

    stunned -> "stunned"

    damaged -> "damaged"

    isBehaviorActive(BehaviorType.JETPACKING) -> amendKey("jetpack")

    isBehaviorActive(BehaviorType.CLIMBING) -> {
        val ladder = body.getProperty(ConstKeys.LADDER, Ladder::class)
        val inLadder = ladder != null && ladder.body.getBounds().contains(body.getCenter())

        when {
            !inLadder && !shooting -> amendKey("climb_finish")
            else -> {
                val movement = if (direction.isHorizontal()) body.physics.velocity.x else body.physics.velocity.y
                val key = if (movement != 0f) "climb" else "climb_still"
                amendKey(key)
            }
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
    shooting -> when (currentWeapon) {
        MegamanWeapon.NEEDLE_SPIN -> "needle_spin"
        MegamanWeapon.AXE_SWINGER -> when {
            isBehaviorActive(BehaviorType.CROUCHING) -> "crouch_axe_throw"
            else -> "stand_axe_throw"
        }
        MegamanWeapon.RODENT_CLAWS -> {
            val slashIndex = getOrDefaultProperty("slash_index", 1, Int::class)
            GameLogger.debug(MEGAMAN_ANIM_KEY_SUPPLIER_TAG, "slashIndex=$slashIndex")
            // TODO: shouldn't always be "stand" animation
            "stand_slash$slashIndex"
        }
        else -> "${baseKey}_shoot"
    }
    fullyCharged -> "${baseKey}_charge_full"
    halfCharged -> "${baseKey}_charge_half"
    else -> baseKey
}

