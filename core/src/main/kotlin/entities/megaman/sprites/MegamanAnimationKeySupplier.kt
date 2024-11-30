package com.megaman.maverick.game.entities.megaman.sprites

import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

fun Megaman.getAnimationKey(priorAnimKey: String) = when {
    !roomTransPauseTimer.isFinished() -> ConstKeys.INVALID

    game.isProperty(ConstKeys.ROOM_TRANSITION, true) -> {
        var key = priorAnimKey
        if (key.contains("Stand")) key = key.replace("Stand", "Run")
        key
    }

    !ready -> "Stand"

    game.isCameraRotating() -> {
        when {
            shooting -> "JumpShoot"
            fullyCharged -> "JumpCharging"
            halfCharged -> "JumpHalfCharging"
            else -> "Jump"
        }
    }

    else -> {
        when {
            isBehaviorActive(BehaviorType.JETPACKING) -> if (shooting) "JetpackShoot" else "Jetpack"
            isBehaviorActive(BehaviorType.RIDING_CART) -> {
                when {
                    damaged -> "Cartin_Damaged"
                    isBehaviorActive(BehaviorType.JUMPING) || !body.isSensing(BodySense.FEET_ON_GROUND) ->
                        when {
                            shooting -> "Cartin_JumpShoot"
                            fullyCharged -> "Cartin_JumpFullyCharged"
                            halfCharged -> "Cartin_JumpHalfCharged"
                            else -> "Cartin_Jump"
                        }

                    else ->
                        when {
                            shooting -> "Cartin_Shoot"
                            fullyCharged -> "Cartin_FullyCharged"
                            halfCharged -> "Cartin_HalfCharged"
                            else -> "Cartin"
                        }
                }
            }

            damaged || stunned -> "Damaged"
            isBehaviorActive(BehaviorType.CLIMBING) -> {
                when {
                    !body.isSensing(BodySense.HEAD_TOUCHING_LADDER) -> {
                        when {
                            shooting -> "ClimbShoot"
                            fullyCharged -> "FinishClimbCharging"
                            halfCharged -> "FinishClimbHalfCharging"
                            else -> "FinishClimb"
                        }
                    }

                    else -> {
                        val movement =
                            if (direction.isHorizontal()) body.physics.velocity.x else body.physics.velocity.y
                        when {
                            movement != 0f -> when {
                                shooting -> "ClimbShoot"
                                fullyCharged -> "ClimbCharging"
                                halfCharged -> "ClimbHalfCharging"
                                else -> "Climb"
                            }

                            else -> when {
                                shooting -> "ClimbShoot"
                                fullyCharged -> "StillClimbCharging"
                                halfCharged -> "StillClimbHalfCharging"
                                else -> "StillClimb"
                            }
                        }
                    }
                }
            }

            isBehaviorActive(BehaviorType.AIR_DASHING) -> when {
                fullyCharged -> "AirDashCharging"
                halfCharged -> "AirDashHalfCharging"
                else -> "AirDash"
            }

            isBehaviorActive(BehaviorType.GROUND_SLIDING) -> when {
                shooting -> "GroundSlideShoot"
                fullyCharged -> "GroundSlideCharging"
                halfCharged -> "GroundSlideHalfCharging"
                else -> "GroundSlide"
            }

            isBehaviorActive(BehaviorType.WALL_SLIDING) -> when {
                shooting -> "WallSlideShoot"
                fullyCharged -> "WallSlideCharging"
                halfCharged -> "WallSlideHalfCharging"
                else -> "WallSlide"
            }

            isBehaviorActive(BehaviorType.SWIMMING) -> when {
                shooting -> "SwimShoot"
                fullyCharged -> "SwimCharging"
                halfCharged -> "SwimHalfCharging"
                else -> "Swim"
            }

            body.isSensing(BodySense.FEET_ON_GROUND) && running -> when {
                shooting -> "RunShoot"
                fullyCharged -> "RunCharging"
                halfCharged -> "RunHalfCharging"
                else -> "Run"
            }

            isBehaviorActive(BehaviorType.JUMPING) || !body.isSensing(BodySense.FEET_ON_GROUND) -> when {
                shooting -> "JumpShoot"
                fullyCharged -> "JumpCharging"
                halfCharged -> "JumpHalfCharging"
                else -> "Jump"
            }

            slipSliding -> when {
                shooting -> "SlipSlideShoot"
                fullyCharged -> "SlipSlideCharging"
                halfCharged -> "SlipSlideHalfCharging"
                else -> "SlipSlide"
            }

            else -> when {
                shooting -> "StandShoot"
                fullyCharged -> "StandCharging"
                halfCharged -> "StandHalfCharging"
                else -> "Stand"
            }
        }
    }
}
