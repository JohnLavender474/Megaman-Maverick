package com.megaman.maverick.game.entities.megaman.behaviors

import com.maverick.game.engine.behaviors.FunctionalBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.input
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.world.body.*

const val MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: WallSlideBehavior"

internal fun Megaman.wallSlideBehavior(): IBehavior {
    return FunctionalBehaviorImpl(
        evaluate = evaluate@{
            if (dead || !inPlay || !canMove || body.isSensing(BodySense.FEET_ON_SAND) ||
                isBehaviorActive(BehaviorType.JETPACKING) || !wallSlideNotAllowedTimer.isFinished() ||
                (currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting)
            ) return@evaluate false

            if ((body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) &&
                    input.isPressed(MegaControllerButton.LEFT)) ||
                (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) &&
                    input.isPressed(MegaControllerButton.RIGHT))
            ) {
                if (damaged) {
                    GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "damaged")
                    return@evaluate false
                }
                if (isBehaviorActive(BehaviorType.JUMPING)) {
                    GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "jumping")
                    return@evaluate false
                }
                if (isBehaviorActive(BehaviorType.CLIMBING)) {
                    GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "climbing")
                    return@evaluate false
                }
                if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                    GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "feet on ground")
                    return@evaluate false
                }
                if (!wallJumpTimer.isFinished()) {
                    GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "wall jump timer not finished")
                    return@evaluate false
                }
                return@evaluate true
            } else return@evaluate false
        },
        init = {
            aButtonTask = AButtonTask.JUMP
            GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "init()")
        },
        act = {
            aButtonTask = AButtonTask.JUMP

            val friction =
                MegamanValues.WALL_SLIDE_FRICTION_TO_APPLY * ConstVals.PPM
            when {
                direction.isVertical() -> body.physics.frictionOnSelf.y = friction
                else -> body.physics.frictionOnSelf.x = friction
            }

            if (currentWeapon == MegamanWeapon.RODENT_CLAWS) {
                when {
                    direction.isVertical() -> body.physics.velocity.y = 0f
                    else -> body.physics.velocity.x = 0f
                }
                body.physics.gravityOn = false
            }
        },
        end = {
            if (!body.isSensing(BodySense.IN_WATER)) aButtonTask = AButtonTask.AIR_DASH
            GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "end()")
            body.physics.gravityOn = true
        })
}
