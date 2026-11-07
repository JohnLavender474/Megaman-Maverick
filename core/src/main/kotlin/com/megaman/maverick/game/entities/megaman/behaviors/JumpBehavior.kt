package com.megaman.maverick.game.entities.megaman.behaviors

import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.players.JumpParams
import com.megaman.maverick.game.players.playerJumpBehavior

internal fun Megaman.jumpBehavior(): IBehavior = playerJumpBehavior(
    JumpParams(
        canStart = { aButtonTask == AButtonTask.JUMP },
        blocked = { currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting },
        jumpSpeed = {
            when {
                services.gameState.containsEnhancement(MegaEnhancement.JUMP_BOOST) ->
                    jumpVel * MegaEnhancement.JUMP_BOOST_SCALAR

                else -> jumpVel
            }
        },
        wallJumpSpeed = { wallJumpVel },
        wallJumpHorizontal = MegamanValues.WALL_JUMP_HORIZONTAL,
        wallJumpAllowed = { isBehaviorActive(BehaviorType.WALL_SLIDING) },
        ignoreRelease = { false },
        startPressed = { input.isJustPressed(MegaControllerButton.A) },
        downBlocksGroundJump = true,
        endKeepsFall = false,
        onJump = {
            if (services.levelState.currentLevel!!.type == LevelType.MARIO_LEVEL)
                requestToPlaySound(SoundAsset.SMB3_JUMP_SOUND, false)
            canMakeLandSound = true
        }
    )
)
