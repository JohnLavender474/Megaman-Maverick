package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.math.Vector2
import com.engine.behaviors.AbstractBehavior
import com.engine.behaviors.Behavior
import com.engine.behaviors.BehaviorsComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.ControllerButton
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.BButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.special.Ladder
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing

const val MEGAMAN_TEST_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: TestBehavior"
const val MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: WallSlideBehavior"
const val MEGAMAN_SWIM_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: SwimBehavior"
const val MEGAMAN_JUMP_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: JumpBehavior"
const val MEGAMAN_AIR_DASH_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: AirDashBehavior"
const val MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: GroundSlideBehavior"

internal fun Megaman.defineBehaviorsComponent(): BehaviorsComponent {
  val behaviorsComponent = BehaviorsComponent(this)

  // test
  val test =
      Behavior(
          evaluate = {
            return@Behavior game.controllerPoller.isButtonPressed(ControllerButton.B.name)
          },
          init = { GameLogger.debug(MEGAMAN_TEST_BEHAVIOR_TAG, "Init method called") },
          end = { GameLogger.debug(MEGAMAN_TEST_BEHAVIOR_TAG, "End method called") })

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
                game.controllerPoller.isButtonPressed(ControllerButton.LEFT.name))
                true
            else
                body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) &&
                    game.controllerPoller.isButtonPressed(ControllerButton.RIGHT.name)
          },
          // init
          init = {
            bButtonTask = BButtonTask.JUMP

            GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Init method called")
          },
          // act
          act = { body.physics.frictionOnSelf.y += 1.25f },
          // end
          end = {
            if (!body.isSensing(BodySense.IN_WATER)) bButtonTask = BButtonTask.AIR_DASH

            GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "End method called")
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
            else {
              val bButtonJustPressed =
                  game.controllerPoller.isButtonJustPressed(ControllerButton.A.name)
              val doSwim = bButtonJustPressed && bButtonTask == BButtonTask.SWIM
              GameLogger.debug(
                  MEGAMAN_SWIM_BEHAVIOR_TAG,
                  "A button just pressed: $bButtonJustPressed. A button task: $bButtonTask. Evaluate method yielding $doSwim")
              doSwim
            }
          },
          // init
          init = {
            body.physics.velocity.y += swimVelY * ConstVals.PPM
            requestToPlaySound(SoundAsset.SWIM_SOUND, false)
            GameLogger.debug(MEGAMAN_SWIM_BEHAVIOR_TAG, "Init method called")
          })

  // jump
  val jump =
      Behavior(
          // evaluate
          evaluate = {
            if (damaged ||
                isAnyBehaviorActive(BehaviorType.SWIMMING, BehaviorType.CLIMBING) ||
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) ||
                !game.controllerPoller.isButtonPressed(ControllerButton.A.name) ||
                game.controllerPoller.isButtonPressed(
                    if (upsideDown) ControllerButton.UP.name else ControllerButton.DOWN.name))
                return@Behavior false

            return@Behavior if (isBehaviorActive(BehaviorType.JUMPING)) {
              val velocity = body.physics.velocity
              if (upsideDown) velocity.y <= 0f else velocity.y >= 0f
            } else
                bButtonTask == BButtonTask.JUMP &&
                    game.controllerPoller.isButtonJustPressed(ControllerButton.A.name) &&
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

            GameLogger.debug(MEGAMAN_JUMP_BEHAVIOR_TAG, "Init method called")
          },
          // end
          end = {
            body.physics.velocity.y = 0f

            GameLogger.debug(MEGAMAN_JUMP_BEHAVIOR_TAG, "End method called")
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
                game.controllerPoller.isButtonPressed(ControllerButton.A.name)
            else
                game.controllerPoller.isButtonJustPressed(ControllerButton.A.name) &&
                    bButtonTask == BButtonTask.AIR_DASH
          },
          // init
          init = {
            body.physics.gravityOn = false
            bButtonTask = BButtonTask.JUMP
            requestToPlaySound(SoundAsset.WHOOSH_SOUND, true)

            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "Init method called")
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

            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "End method called")
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
                    if (upsideDown) ControllerButton.UP.name else ControllerButton.DOWN.name))
                return@Behavior false

            return@Behavior if (isBehaviorActive(BehaviorType.GROUND_SLIDING))
                game.controllerPoller.isButtonPressed(ControllerButton.A.name)
            else game.controllerPoller.isButtonJustPressed(ControllerButton.A.name)
          },
          // init
          init = {
            // In body pre-process, body height is reduced from .95f to .45f when ground sliding;
            // when upside down, need to compensate, otherwise Megaman will be off the ground
            if (upsideDown) body.y += .5f * ConstVals.PPM

            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "Init method called")
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

            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "End method called")
          })

  // climb
  val climb =
      object : AbstractBehavior() {

        private lateinit var ladder: Ladder

        override fun evaluate(delta: Float): Boolean {
          if (damaged ||
              isAnyBehaviorActive(
                  BehaviorType.JUMPING, BehaviorType.AIR_DASHING, BehaviorType.GROUND_SLIDING) ||
              !body.properties.containsKey(ConstKeys.LADDER))
              return false

          ladder = body.properties.get(ConstKeys.LADDER, Ladder::class)!!

          val centerY = body.getCenter().y

          if (isBehaviorActive(BehaviorType.CLIMBING)) {
            if (body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) {
              if (upsideDown && centerY + 0.15f * ConstVals.PPM < ladder.body.y) return false
              else if (centerY - 0.15f * ConstVals.PPM > ladder.body.getMaxY()) return false
            }

            if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) {
              if (upsideDown && centerY - 0.15f * ConstVals.PPM > ladder.body.getMaxY())
                  return false
              else if (centerY + 0.15f * ConstVals.PPM < ladder.body.y) return false
            }

            if (game.controllerPoller.isButtonJustPressed(ControllerButton.A.name)) return false

            return true
          }

          if (body.isSensing(BodySense.FEET_TOUCHING_LADDER) &&
              game.controllerPoller.isButtonPressed(
                  if (upsideDown) ControllerButton.UP.name else ControllerButton.DOWN.name))
              return true

          if (body.isSensing(BodySense.HEAD_TOUCHING_LADDER) &&
              game.controllerPoller.isButtonPressed(
                  if (upsideDown) ControllerButton.DOWN.name else ControllerButton.UP.name))
              return true

          return false
        }

        override fun init() {
          bButtonTask =
              if (body.isSensing(BodySense.IN_WATER)) BButtonTask.SWIM else BButtonTask.AIR_DASH

          body.physics.gravityOn = false
          // TODO: body.physics.collisionOn = false
          body.setCenterX(ladder.body.getCenter().x)

          if (body.getMaxY() <= ladder.body.y) body.setY(ladder.body.y)
          else if (body.y >= ladder.body.getMaxY()) body.setMaxY(ladder.body.getMaxY())

          body.physics.velocity.setZero()
        }

        override fun act(delta: Float) {
          body.setCenterX(ladder.body.getCenter().x)

          if (shooting) {
            body.physics.velocity.setZero()
            return
          }

          if (game.controllerPoller.isButtonPressed(ControllerButton.UP.name))
              body.physics.velocity.y = MegamanValues.CLIMB_VEL * ConstVals.PPM
          else if (game.controllerPoller.isButtonPressed(ControllerButton.DOWN.name))
              body.physics.velocity.y = MegamanValues.CLIMB_VEL * ConstVals.PPM * -1f
          else body.physics.velocity.y = 0f
        }

        override fun end() {
          body.physics.gravityOn = true
          // TODO: body.physics.collisionOn = true
          body.physics.velocity.setZero()
          bButtonTask =
              if (body.isSensing(BodySense.IN_WATER)) BButtonTask.SWIM else BButtonTask.AIR_DASH
        }
      }

  behaviorsComponent.addBehavior(BehaviorType.TEST, test)
  behaviorsComponent.addBehavior(BehaviorType.WALL_SLIDING, wallSlide)
  behaviorsComponent.addBehavior(BehaviorType.SWIMMING, swim)
  behaviorsComponent.addBehavior(BehaviorType.JUMPING, jump)
  behaviorsComponent.addBehavior(BehaviorType.AIR_DASHING, airDash)
  behaviorsComponent.addBehavior(BehaviorType.GROUND_SLIDING, groundSlide)
  behaviorsComponent.addBehavior(BehaviorType.CLIMBING, climb)

  return behaviorsComponent
}
