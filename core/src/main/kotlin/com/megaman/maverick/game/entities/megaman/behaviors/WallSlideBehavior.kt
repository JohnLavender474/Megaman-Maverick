package com.megaman.maverick.game.entities.megaman.behaviors

import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.weapons.currentWeaponPrevents
import com.megaman.maverick.game.players.WallSlideParams
import com.megaman.maverick.game.players.playerWallSlideBehavior
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

internal fun Megaman.wallSlideBehavior(): IBehavior = playerWallSlideBehavior(
    this,
    WallSlideParams(
        blocked = {
            body.isSensing(BodySense.FEET_ON_SAND) || isBehaviorActive(BehaviorType.JETPACKING) ||
                !wallSlideNotAllowedTimer.isFinished() || currentWeaponPrevents(BehaviorType.WALL_SLIDING) ||
                !wallJumpTimer.isFinished()
        },
        allowedWhileJumping = { false },
        holdsWithoutInput = { false },
        onStart = { aButtonTask = AButtonTask.JUMP },
        onAct = {
            aButtonTask = AButtonTask.JUMP

            val friction = MegamanValues.WALL_SLIDE_FRICTION_TO_APPLY * ConstVals.PPM
            when {
                direction.isVertical() -> body.physics.frictionOnSelf.y = friction
                else -> body.physics.frictionOnSelf.x = friction
            }

            if (currentWeapon.gripsWall) {
                when {
                    direction.isVertical() -> body.physics.velocity.y = 0f
                    else -> body.physics.velocity.x = 0f
                }
                body.physics.gravityOn = false
            }
        },
        onEnd = { if (!body.isSensing(BodySense.IN_WATER)) aButtonTask = AButtonTask.AIR_DASH }
    )
)
