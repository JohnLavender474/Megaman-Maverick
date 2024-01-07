package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.math.Vector2
import com.engine.behaviors.AbstractBehavior
import com.engine.behaviors.Behavior
import com.engine.behaviors.BehaviorsComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.interfaces.isFacing
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.ControllerButton
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanKeys
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.special.Ladder
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing

const val MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: WallSlideBehavior"
const val MEGAMAN_SWIM_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: SwimBehavior"
const val MEGAMAN_JUMP_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: JumpBehavior"
const val MEGAMAN_AIR_DASH_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: AirDashBehavior"
const val MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: GroundSlideBehavior"

internal fun Megaman.defineBehaviorsComponent(): BehaviorsComponent {
  val behaviorsComponent = BehaviorsComponent(this)

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
                game.controllerPoller.isPressed(ControllerButton.LEFT))
                true
            else
                body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) &&
                    game.controllerPoller.isPressed(ControllerButton.RIGHT)
          },
          // init
          init = {
            aButtonTask = AButtonTask.JUMP
            GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Init method called")
          },
          // act
          act = {
            if (isDirectionRotatedVertically()) body.physics.frictionOnSelf.y += 1.2f
            else body.physics.frictionOnSelf.x += 1.2f
          },
          // end
          end = {
            if (!body.isSensing(BodySense.IN_WATER)) aButtonTask = AButtonTask.AIR_DASH
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
                when (directionRotation) {
                  Direction.UP -> body.physics.velocity.y > 0f
                  Direction.DOWN -> body.physics.velocity.y < 0f
                  Direction.LEFT -> body.physics.velocity.x < 0f
                  Direction.RIGHT -> body.physics.velocity.x > 0f
                }
            else {
              val aButtonJustPressed = game.controllerPoller.isJustPressed(ControllerButton.A)
              val doSwim = aButtonJustPressed && aButtonTask == AButtonTask.SWIM
              GameLogger.debug(
                  MEGAMAN_SWIM_BEHAVIOR_TAG,
                  "A button just pressed: $aButtonJustPressed. A button task: $aButtonTask. Evaluate method yielding $doSwim")
              doSwim
            }
          },
          // init
          init = {
            body.physics.velocity.add(
                when (directionRotation) {
                  Direction.UP -> Vector2(0f, swimVel * ConstVals.PPM)
                  Direction.DOWN -> Vector2(0f, -swimVel * ConstVals.PPM)
                  Direction.LEFT -> Vector2(-swimVel * ConstVals.PPM, 0f)
                  Direction.RIGHT -> Vector2(swimVel * ConstVals.PPM, 0f)
                })
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
                !game.controllerPoller.isPressed(ControllerButton.A) ||
                game.controllerPoller.isPressed(ControllerButton.DOWN))
                return@Behavior false

            return@Behavior if (isBehaviorActive(BehaviorType.JUMPING)) {
              val velocity = body.physics.velocity
              when (directionRotation) {
                Direction.UP -> velocity.y >= 0f
                Direction.DOWN -> velocity.y <= 0f
                Direction.LEFT -> velocity.x <= 0f
                Direction.RIGHT -> velocity.x >= 0f
              }
            } else
                aButtonTask == AButtonTask.JUMP &&
                    game.controllerPoller.isJustPressed(ControllerButton.A) &&
                    (body.isSensing(BodySense.FEET_ON_GROUND) ||
                        isBehaviorActive(BehaviorType.WALL_SLIDING))
          },
          // init
          init = {
            val v = Vector2()
            v.x =
                when (directionRotation) {
                  Direction.UP,
                  Direction.DOWN -> {
                    if (isBehaviorActive(BehaviorType.WALL_SLIDING))
                        MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM * facing.value
                    else body.physics.velocity.x
                  }
                  Direction.LEFT,
                  Direction.RIGHT -> {
                    ConstVals.PPM *
                        if (body.isSensing(BodySense.IN_WATER))
                            (if (isBehaviorActive(BehaviorType.WALL_SLIDING)) waterWallJumpVel
                            else waterJumpVel)
                        else
                            (if (isBehaviorActive(BehaviorType.WALL_SLIDING)) wallJumpVel
                            else jumpVel)
                  }
                }
            v.y =
                when (directionRotation) {
                  Direction.UP,
                  Direction.DOWN -> {
                    ConstVals.PPM *
                        if (body.isSensing(BodySense.IN_WATER))
                            (if (isBehaviorActive(BehaviorType.WALL_SLIDING)) waterWallJumpVel
                            else waterJumpVel)
                        else
                            (if (isBehaviorActive(BehaviorType.WALL_SLIDING)) wallJumpVel
                            else jumpVel)
                  }
                  Direction.LEFT,
                  Direction.RIGHT -> {
                    if (isBehaviorActive(BehaviorType.WALL_SLIDING))
                        MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM * facing.value
                    else body.physics.velocity.y
                  }
                }
            body.physics.velocity.set(v)
            requestToPlaySound(SoundAsset.WALL_JUMP, false)
            GameLogger.debug(MEGAMAN_JUMP_BEHAVIOR_TAG, "Init method called")
          },
          // end
          end = {
            if (isDirectionRotatedVertically()) body.physics.velocity.y = 0f
            else body.physics.velocity.x = 0f
            GameLogger.debug(MEGAMAN_JUMP_BEHAVIOR_TAG, "End method called")
          })

  // air dash
  val airDash =
      object : AbstractBehavior() {

        private var lastFacing = Facing.RIGHT
        private val impulse = Vector2()

        override fun evaluate(delta: Float): Boolean {
          if (damaged ||
              airDashTimer.isFinished() ||
              body.isSensing(BodySense.FEET_ON_GROUND) ||
              isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.CLIMBING))
              return false

          return if (isBehaviorActive(BehaviorType.AIR_DASHING))
              game.controllerPoller.isPressed(ControllerButton.A)
          else
              game.controllerPoller.isJustPressed(ControllerButton.A) &&
                  aButtonTask == AButtonTask.AIR_DASH
        }

        override fun init() {
          GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "Init")
          body.physics.gravityOn = false
          aButtonTask = AButtonTask.JUMP
          requestToPlaySound(SoundAsset.WHOOSH_SOUND, false)

          if (isDirectionRotatedVertically()) impulse.y = 0f else impulse.x = 0f

          val impulseValue =
              facing.value *
                  ConstVals.PPM *
                  if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_VEL
                  else MegamanValues.AIR_DASH_VEL

          when (directionRotation) {
            Direction.UP,
            Direction.DOWN -> impulse.x = impulseValue
            Direction.LEFT,
            Direction.RIGHT -> impulse.y = impulseValue
          }

          lastFacing = facing

          putProperty(MegamanKeys.DIRECTION_ON_AIR_DASH, directionRotation)
        }

        override fun act(delta: Float) {
          airDashTimer.update(delta)
          if (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
              isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
              return

          if (facing != lastFacing) impulse.scl(-1f)
          lastFacing = facing

          body.physics.velocity.set(impulse)
        }

        override fun end() {
          GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "End")
          airDashTimer.reset()
          body.physics.gravityOn = true

          val impulseOnEnd =
              facing.value *
                  ConstVals.PPM *
                  if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_END_BUMP
                  else MegamanValues.AIR_DASH_END_BUMP

          when (directionRotation) {
            Direction.UP -> body.physics.velocity.x += impulseOnEnd
            Direction.DOWN -> body.physics.velocity.x -= impulseOnEnd
            Direction.LEFT -> body.physics.velocity.y += impulseOnEnd
            Direction.RIGHT -> body.physics.velocity.y -= impulseOnEnd
          }
        }
      }

  /*
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
                game.controllerPoller.isPressed(ControllerButton.A)
            else
                game.controllerPoller.isJustPressed(ControllerButton.A) &&
                    aButtonTask == AButtonTask.AIR_DASH
          },
          // init
          init = {
            body.physics.gravityOn = false
            aButtonTask = AButtonTask.JUMP
            requestToPlaySound(SoundAsset.WHOOSH_SOUND, false)
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "Init method called")
          },
          // act
          act = {
            airDashTimer.update(it)

            if (isDirectionRotatedVertically()) body.physics.velocity.y = 0f
            else body.physics.velocity.x = 0f

            if (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                return@Behavior

            val impulse =
                facing.value *
                    ConstVals.PPM *
                    if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_VEL
                    else MegamanValues.AIR_DASH_VEL

            when (directionRotation) {
              Direction.UP,
              Direction.DOWN -> body.physics.velocity.x = impulse
              Direction.LEFT,
              Direction.RIGHT -> body.physics.velocity.y = impulse
            }
          },
          // end
          end = {
            airDashTimer.reset()
            body.physics.gravityOn = true
            val impulse =
                facing.value *
                    ConstVals.PPM *
                    if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_END_BUMP
                    else MegamanValues.AIR_DASH_END_BUMP
            when (directionRotation) {
              Direction.UP -> body.physics.velocity.x += impulse
              Direction.DOWN -> body.physics.velocity.x -= impulse
              Direction.LEFT -> body.physics.velocity.y += impulse
              Direction.RIGHT -> body.physics.velocity.y -= impulse
            }
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "End method called")
          })
     */

  // ground slide
  val groundSlide =
      object : AbstractBehavior() {

        private var directionOnInit: Direction? = null

        override fun evaluate(delta: Float): Boolean {
          if (isBehaviorActive(BehaviorType.GROUND_SLIDING) &&
              body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))
              return true

          if (damaged ||
              groundSlideTimer.isFinished() ||
              !body.isSensing(BodySense.FEET_ON_GROUND) ||
              !game.controllerPoller.isPressed(ControllerButton.DOWN))
              return false

          return if (isBehaviorActive(BehaviorType.GROUND_SLIDING))
              game.controllerPoller.isPressed(ControllerButton.A) &&
                  directionOnInit == directionRotation
          else game.controllerPoller.isJustPressed(ControllerButton.A)
        }

        override fun init() {
          // In body pre-process, body height is reduced from .95f to .45f when ground sliding;
          // when upside down, need to compensate, otherwise Megaman will be off the ground
          when (directionRotation) {
            Direction.UP -> {}
            Direction.DOWN -> body.y += ConstVals.PPM / 2f
            Direction.LEFT -> body.x += ConstVals.PPM / 2f
            Direction.RIGHT -> body.x -= ConstVals.PPM / 2f
          }

          GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "Init method called")

          directionOnInit = directionRotation
        }

        override fun act(delta: Float) {
          groundSlideTimer.update(delta)

          if (damaged ||
              isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
              isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
              return

          val impulse =
              (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_GROUND_SLIDE_VEL
              else MegamanValues.GROUND_SLIDE_VEL) * ConstVals.PPM * facing.value

          when (directionRotation) {
            Direction.UP,
            Direction.DOWN -> body.physics.velocity.x = impulse
            Direction.LEFT,
            Direction.RIGHT -> body.physics.velocity.y = impulse
          }
        }

        override fun end() {
          groundSlideTimer.reset()

          val endDash =
              (if (body.isSensing(BodySense.IN_WATER)) 2f else 5f) * ConstVals.PPM * facing.value

          if (directionOnInit == directionRotation) {
            when (directionRotation) {
              Direction.UP,
              Direction.DOWN -> body.physics.velocity.x += endDash
              Direction.LEFT,
              Direction.RIGHT -> body.physics.velocity.y += endDash
            }
          } else {
            body.physics.velocity.setZero()
            when (directionOnInit) {
              Direction.UP,
              Direction.DOWN -> body.physics.velocity.x = endDash
              Direction.LEFT,
              Direction.RIGHT -> body.physics.velocity.y = endDash
              null -> throw IllegalStateException("Direction on init cannot be null")
            }
          }

          GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "End method called")
        }
      }

  /*
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
                !game.controllerPoller.isPressed(ControllerButton.DOWN))
                return@Behavior false

            return@Behavior if (isBehaviorActive(BehaviorType.GROUND_SLIDING))
                game.controllerPoller.isPressed(ControllerButton.A)
            else game.controllerPoller.isJustPressed(ControllerButton.A)
          },
          // init
          init = {
            // In body pre-process, body height is reduced from .95f to .45f when ground sliding;
            // when upside down, need to compensate, otherwise Megaman will be off the ground
            when (directionRotation) {
              Direction.UP -> {}
              Direction.DOWN -> body.y += ConstVals.PPM / 2f
              Direction.LEFT -> body.x += ConstVals.PPM / 2f
              Direction.RIGHT -> body.x -= ConstVals.PPM / 2f
            }

            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "Init method called")
          },
          // act
          act = {
            groundSlideTimer.update(it)

            if (damaged ||
                isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                return@Behavior

            val impulse =
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_GROUND_SLIDE_VEL
                else MegamanValues.GROUND_SLIDE_VEL) * ConstVals.PPM * facing.value

            when (directionRotation) {
              Direction.UP,
              Direction.DOWN -> body.physics.velocity.x = impulse
              Direction.LEFT,
              Direction.RIGHT -> body.physics.velocity.y = impulse
            }
          },
          // end
          end = {
            groundSlideTimer.reset()
            val endDash =
                (if (body.isSensing(BodySense.IN_WATER)) 2f else 5f) * ConstVals.PPM * facing.value

            when (directionRotation) {
              Direction.UP,
              Direction.DOWN -> body.physics.velocity.x += endDash
              Direction.LEFT,
              Direction.RIGHT -> body.physics.velocity.y += endDash
            }

            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "End method called")
          })

     */

  // TODO: modify climb behavior for sideways Megaman
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
              if (isDirectionRotatedDown() && centerY + 0.15f * ConstVals.PPM < ladder.body.y)
                  return false
              else if (centerY - 0.15f * ConstVals.PPM > ladder.body.getMaxY()) return false
            }

            if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) {
              if (isDirectionRotatedDown() &&
                  centerY - 0.15f * ConstVals.PPM > ladder.body.getMaxY())
                  return false
              else if (centerY + 0.15f * ConstVals.PPM < ladder.body.y) return false
            }

            if (game.controllerPoller.isJustPressed(ControllerButton.A)) return false

            return true
          }

          if (body.isSensing(BodySense.FEET_TOUCHING_LADDER) &&
              game.controllerPoller.isPressed(
                  if (isDirectionRotatedDown()) ControllerButton.UP else ControllerButton.DOWN))
              return true

          if (body.isSensing(BodySense.HEAD_TOUCHING_LADDER) &&
              game.controllerPoller.isPressed(
                  if (isDirectionRotatedDown()) ControllerButton.DOWN else ControllerButton.UP))
              return true

          return false
        }

        override fun init() {
          aButtonTask =
              if (body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM else AButtonTask.AIR_DASH

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

          if (game.controllerPoller.isPressed(ControllerButton.UP))
              body.physics.velocity.y = MegamanValues.CLIMB_VEL * ConstVals.PPM
          else if (game.controllerPoller.isPressed(ControllerButton.DOWN))
              body.physics.velocity.y = MegamanValues.CLIMB_VEL * ConstVals.PPM * -1f
          else body.physics.velocity.y = 0f
        }

        override fun end() {
          body.physics.gravityOn = true
          // TODO: body.physics.collisionOn = true
          body.physics.velocity.setZero()
          aButtonTask =
              if (body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM else AButtonTask.AIR_DASH
        }
      }

  behaviorsComponent.addBehavior(BehaviorType.WALL_SLIDING, wallSlide)
  behaviorsComponent.addBehavior(BehaviorType.SWIMMING, swim)
  behaviorsComponent.addBehavior(BehaviorType.JUMPING, jump)
  behaviorsComponent.addBehavior(BehaviorType.AIR_DASHING, airDash)
  behaviorsComponent.addBehavior(BehaviorType.GROUND_SLIDING, groundSlide)
  behaviorsComponent.addBehavior(BehaviorType.CLIMBING, climb)

  return behaviorsComponent
}
