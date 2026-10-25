package com.megaman.maverick.game.entities.megaman.behaviors

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.behaviors.FunctionalBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.bodyOverGround
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

const val MEGAMAN_JUMP_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: JumpBehavior"

internal fun Megaman.jumpBehavior(): IBehavior {
    return FunctionalBehaviorImpl(
        evaluate = {
            if (dead || !ready || !canMove || damaged || teleporting ||
                isAnyBehaviorActive(BehaviorType.SWIMMING, BehaviorType.CLIMBING, BehaviorType.JETPACKING) ||
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) ||
                !game.controllerPoller.isPressed(MegaControllerButton.A) ||
                (bodyOverGround && game.controllerPoller.isPressed(MegaControllerButton.DOWN)) ||
                (currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting)
            ) return@FunctionalBehaviorImpl false

            if (isBehaviorActive(BehaviorType.JUMPING)) {
                val velocity = body.physics.velocity
                return@FunctionalBehaviorImpl when (direction) {
                    Direction.UP -> velocity.y > 0f
                    Direction.DOWN -> velocity.y < 0f
                    Direction.LEFT -> velocity.x < 0f
                    Direction.RIGHT -> velocity.x > 0f
                }
            }

            return@FunctionalBehaviorImpl (
                aButtonTask == AButtonTask.JUMP &&
                    game.controllerPoller.isJustPressed(MegaControllerButton.A) &&
                    (bodyOverGround || isBehaviorActive(BehaviorType.WALL_SLIDING))
                )
        },
        init = {
            when {
                direction.isVertical() -> body.physics.frictionOnSelf.y = 1f
                else -> body.physics.frictionOnSelf.x = 1f
            }

            val v = GameObjectPools.fetch(Vector2::class)
            v.x = when (direction) {
                Direction.UP, Direction.DOWN -> when {
                    isBehaviorActive(BehaviorType.WALL_SLIDING) ||
                        (body.isSensingAny(
                            BodySense.SIDE_TOUCHING_BLOCK_LEFT,
                            BodySense.SIDE_TOUCHING_BLOCK_RIGHT
                        ) && !body.isSensing(BodySense.FEET_ON_GROUND)) ->
                        MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM * facing.value *
                            (if (direction == Direction.UP) 1f else -1f)

                    else -> body.physics.velocity.x
                }

                Direction.LEFT, Direction.RIGHT -> ConstVals.PPM * when {
                    isBehaviorActive(BehaviorType.WALL_SLIDING) -> wallJumpVel
                    game.state.containsEnhancement(MegaEnhancement.JUMP_BOOST) -> jumpVel * MegaEnhancement.JUMP_BOOST_SCALAR
                    else -> jumpVel
                }
            }
            v.y = when (direction) {
                Direction.UP, Direction.DOWN -> ConstVals.PPM * when {
                    isBehaviorActive(BehaviorType.WALL_SLIDING) -> wallJumpVel
                    game.state.containsEnhancement(MegaEnhancement.JUMP_BOOST) -> jumpVel * MegaEnhancement.JUMP_BOOST_SCALAR
                    else -> jumpVel
                }

                Direction.LEFT -> when {
                    isBehaviorActive(BehaviorType.WALL_SLIDING) ||
                        (body.isSensingAny(
                            BodySense.SIDE_TOUCHING_BLOCK_LEFT,
                            BodySense.SIDE_TOUCHING_BLOCK_RIGHT
                        ) && !body.isSensing(BodySense.FEET_ON_GROUND)) ->
                        MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM * facing.value *
                            (if (direction == Direction.LEFT) 1f else -1f)

                    else -> body.physics.velocity.y
                }

                Direction.RIGHT -> when {
                    isBehaviorActive(BehaviorType.WALL_SLIDING) ||
                        (body.isSensingAny(
                            BodySense.SIDE_TOUCHING_BLOCK_LEFT,
                            BodySense.SIDE_TOUCHING_BLOCK_RIGHT
                        ) && !body.isSensing(BodySense.FEET_ON_GROUND)) ->
                        MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM * -facing.value

                    else -> body.physics.velocity.y
                }
            }
            body.physics.velocity.let {
                when {
                    body.isSensingAll(BodySense.IN_WATER, BodySense.FORCE_APPLIED) -> it.add(v)
                    else -> it.set(v)
                }
            }

            if (game.getCurrentLevel().type == LevelType.MARIO_LEVEL)
                requestToPlaySound(SoundAsset.SMB3_JUMP_SOUND, false)
            canMakeLandSound = true

            stopMomentum(false)

            GameLogger.debug(MEGAMAN_JUMP_BEHAVIOR_TAG, "init(): velocity=$v")
        },
        end = {
            when {
                direction.isVertical() -> body.physics.velocity.y = 0f
                else -> body.physics.velocity.x = 0f
            }

            GameLogger.debug(MEGAMAN_JUMP_BEHAVIOR_TAG, "end(): velocity=${body.physics.velocity}")
        })
}
