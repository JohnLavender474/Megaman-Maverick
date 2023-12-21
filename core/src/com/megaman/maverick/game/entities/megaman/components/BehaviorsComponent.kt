package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.math.Vector2
import com.engine.behaviors.Behavior
import com.engine.behaviors.BehaviorsComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.controller.buttons.ButtonStatus
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing

internal fun Megaman.defineBehaviorsComponent(): BehaviorsComponent {
  val behaviorsComponent = BehaviorsComponent(this)

  // test
  val test =
      Behavior(
          evaluate = {
            return@Behavior game.controllerPoller.isButtonPressed(ConstKeys.B)
          },
          init = { GameLogger.debug("Megaman: TestBehavior", "Init method called") },
          end = { GameLogger.debug("Megaman: TestBehavior", "End method called") })

  // wall slide
  val wallSlide =
      Behavior(
          // evaluate
          evaluate = {
            if (damaged ||
                isAnyBehaviorActive(BehaviorType.JUMPING, BehaviorType.CLIMBING) ||
                body.isSensing(BodySense.FEET_ON_GROUND) ||
                !wallJumpTimer.isFinished() /* TODO: || !upgradeHandler.has(wall_slide) */)
                return@Behavior false

            return@Behavior if (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) &&
                game.controllerPoller.isButtonPressed(ConstKeys.LEFT))
                true
            else
                body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) &&
                    game.controllerPoller.isButtonPressed(ConstKeys.RIGHT)
          },
          // init
          init = {
            aButtonTask = AButtonTask.JUMP

            GameLogger.debug("Megaman: WallSlideBehavior", "Init method called")
          },
          // act
          act = { body.physics.frictionOnSelf.y += 1.25f },
          // end
          end = {
            if (!body.isSensing(BodySense.IN_WATER)) aButtonTask = AButtonTask.AIR_DASH

            GameLogger.debug("Megaman: WallSlideBehavior", "End method called")
          })

  // swim
  val swim =
      Behavior(
          // evaluate
          evaluate = {
            if (damaged ||
                !body.isSensing(BodySense.IN_WATER) ||
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))
                return@Behavior false

            return@Behavior if (isBehaviorActive(BehaviorType.SWIMMING))
                body.physics.velocity.y > 0f
            else
                game.controllerPoller.getButtonStatus(ConstKeys.A) == ButtonStatus.JUST_PRESSED &&
                    aButtonTask == AButtonTask.SWIM
          },
          // init
          init = {
            body.physics.velocity.y += swimVelY * ConstVals.PPM

            var x = MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM
            if (facing == Facing.LEFT) x *= -1f
            body.physics.velocity.x = x

            requestToPlaySound(SoundAsset.SWIM_SOUND, false)

            GameLogger.debug("Megaman: SwimBehavior", "Init method called")
          })

  // jump
  val jump =
      Behavior(
          // evaluate
          evaluate = {
            if (damaged ||
                isAnyBehaviorActive(BehaviorType.SWIMMING, BehaviorType.CLIMBING) ||
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) ||
                !game.controllerPoller.isButtonPressed(ConstKeys.B) ||
                game.controllerPoller.isButtonPressed(
                    if (upsideDown) ConstKeys.UP else ConstKeys.DOWN))
                return@Behavior false

            return@Behavior if (isBehaviorActive(BehaviorType.JUMPING)) {
              val velocity = body.physics.velocity
              if (upsideDown) velocity.y <= 0f else velocity.y >= 0f
            } else
                aButtonTask == AButtonTask.JUMP &&
                    game.controllerPoller.getButtonStatus(ConstKeys.B) ==
                        ButtonStatus.JUST_PRESSED &&
                    (body.isSensing(BodySense.FEET_ON_GROUND) ||
                        isBehaviorActive(BehaviorType.WALL_SLIDING))
          },
          // init
          init = {
            val v = Vector2()

            v.x =
                if (isBehaviorActive(BehaviorType.WALL_SLIDING)) {
                  var x = MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM
                  if (facing == Facing.LEFT) x *= -1
                  x
                } else body.physics.velocity.x

            v.y =
                if (body.isSensing(BodySense.IN_WATER))
                    (if (isBehaviorActive(BehaviorType.WALL_SLIDING)) waterWallJumpVel
                    else waterJumpVel)
                else (if (isBehaviorActive(BehaviorType.WALL_SLIDING)) wallJumpVel else jumpVel)
            v.y *= ConstVals.PPM
            body.physics.velocity.set(v)

            requestToPlaySound(SoundAsset.WALL_JUMP, false)

            GameLogger.debug("Megaman: JumpBehavior", "Init method called")
          },
          // end
          end = {
            body.physics.velocity.y = 0f

            GameLogger.debug("Megaman: JumpBehavior", "End method called")
          })

  // air dash
  val airDash =
      Behavior(
          // evaluate
          evaluate = {
            if (damaged ||
                airDashTimer.isFinished() ||
                body.isSensing(BodySense.FEET_ON_GROUND) ||
                isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.CLIMBING))
                return@Behavior false

            return@Behavior if (isBehaviorActive(BehaviorType.AIR_DASHING))
                game.controllerPoller.isButtonPressed(ConstKeys.B)
            else
                game.controllerPoller.getButtonStatus(ConstKeys.B) == ButtonStatus.JUST_PRESSED &&
                    aButtonTask == AButtonTask.AIR_DASH
          },
          // init
          init = {
            body.physics.gravityOn = false
            aButtonTask = AButtonTask.JUMP
            requestToPlaySound(SoundAsset.WHOOSH_SOUND, true)

            GameLogger.debug("Megaman: AirDashBehavior", "Init method called")
          },
          // act
          act = {
            airDashTimer.update(it)

            body.physics.velocity.y = 0f

            if (facing == Facing.LEFT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                facing == Facing.RIGHT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                return@Behavior

            var x =
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_VEL
                else MegamanValues.AIR_DASH_VEL) * ConstVals.PPM
            if (facing == Facing.LEFT) x *= -1f

            body.physics.velocity.x = x
          },
          // end
          end = {
            airDashTimer.reset()

            body.physics.gravityOn = true

            var x =
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_END_BUMP
                else MegamanValues.AIR_DASH_END_BUMP) * ConstVals.PPM
            if (facing == Facing.LEFT) x *= -1f

            body.physics.velocity.x += x

            GameLogger.debug("Megaman: AirDashBehavior", "End method called")
          })

  // ground slide
  val groundSlide =
      Behavior(
          // evaluate
          evaluate = {
            if (isBehaviorActive(BehaviorType.GROUND_SLIDING) &&
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))
                return@Behavior true

            if (damaged ||
                groundSlideTimer.isFinished() ||
                !body.isSensing(BodySense.FEET_ON_GROUND) ||
                !game.controllerPoller.isButtonPressed(
                    if (upsideDown) ConstKeys.UP else ConstKeys.DOWN))
                return@Behavior false

            return@Behavior if (isBehaviorActive(BehaviorType.GROUND_SLIDING))
                game.controllerPoller.isButtonPressed(ConstKeys.B)
            else game.controllerPoller.getButtonStatus(ConstKeys.B) == ButtonStatus.JUST_PRESSED
          },
          // init
          init = {
            // In body pre-process, body height is reduced from .95f to .45f when ground sliding;
            // when upside down, need to compensate, otherwise Megaman will be off the ground
            if (upsideDown) body.y += .5f * ConstVals.PPM

            GameLogger.debug("Megaman: GroundSlideBehavior", "Init method called")
          },
          // act
          act = {
            groundSlideTimer.update(it)

            if (damaged ||
                facing == Facing.LEFT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                facing == Facing.RIGHT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                return@Behavior

            var x =
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_GROUND_SLIDE_VEL
                else MegamanValues.GROUND_SLIDE_VEL) * ConstVals.PPM
            if (facing == Facing.LEFT) x *= -1f

            body.physics.velocity.x = x
          },
          // end
          end = {
            groundSlideTimer.reset()
            var endDash = (if (body.isSensing(BodySense.IN_WATER)) 2f else 5f) * ConstVals.PPM
            if (facing == Facing.LEFT) endDash *= -1f

            body.physics.velocity.x += endDash

            GameLogger.debug("Megaman: GroundSlideBehavior", "End method called")
          })

  // TODO: implement climb behavior

  behaviorsComponent.addBehavior(BehaviorType.TEST, test)
  behaviorsComponent.addBehavior(BehaviorType.WALL_SLIDING, wallSlide)
  behaviorsComponent.addBehavior(BehaviorType.SWIMMING, swim)
  behaviorsComponent.addBehavior(BehaviorType.JUMPING, jump)
  behaviorsComponent.addBehavior(BehaviorType.AIR_DASHING, airDash)
  behaviorsComponent.addBehavior(BehaviorType.GROUND_SLIDING, groundSlide)

  return behaviorsComponent
}
