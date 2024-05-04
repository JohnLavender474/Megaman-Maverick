package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.math.Vector2
import com.engine.behaviors.AbstractBehavior
import com.engine.behaviors.Behavior
import com.engine.behaviors.BehaviorsComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.interfaces.Updatable
import com.engine.common.interfaces.isFacing
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.ControllerButton
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanKeys
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.special.Cart
import com.megaman.maverick.game.entities.special.Ladder
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing
import com.megaman.maverick.game.world.isSensingAny

const val MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: WallSlideBehavior"
const val MEGAMAN_SWIM_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: SwimBehavior"
const val MEGAMAN_JUMP_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: JumpBehavior"
const val MEGAMAN_AIR_DASH_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: AirDashBehavior"
const val MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: GroundSlideBehavior"

internal fun Megaman.defineBehaviorsComponent(): BehaviorsComponent {
    val behaviorsComponent = BehaviorsComponent(this)

    val wallSlide = Behavior(evaluate = {
        if (!ready || !canMove) return@Behavior false

        if ((body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && game.controllerPoller.isPressed(
                if (isDirectionRotatedDown() || isDirectionRotatedRight()) ControllerButton.RIGHT
                else ControllerButton.LEFT
            )) || body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && game.controllerPoller.isPressed(
                if (isDirectionRotatedDown() || isDirectionRotatedRight()) ControllerButton.LEFT
                else ControllerButton.RIGHT
            )
        ) {
            if (damaged) {
                GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Damaged")
                return@Behavior false
            }
            if (isBehaviorActive(BehaviorType.JUMPING)) {
                GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Jumping")
                return@Behavior false
            }
            if (isBehaviorActive(BehaviorType.CLIMBING)) {
                GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Climbing")
                return@Behavior false
            }
            if (isBehaviorActive(BehaviorType.RIDING_CART)) {
                GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Riding cart")
                return@Behavior false
            }
            if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Feet on ground")
                return@Behavior false
            }
            if (!wallJumpTimer.isFinished()) {
                GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Wall jump timer not finished")
                return@Behavior false
            }
            return@Behavior true
        } else return@Behavior false
    },
        init = {
            aButtonTask = AButtonTask.JUMP
            GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Init method called")
        },
        act = {
            if (isDirectionRotatedVertically()) body.physics.frictionOnSelf.y += 1.2f
            else body.physics.frictionOnSelf.x += 1.2f
        },
        end = {
            if (!body.isSensing(BodySense.IN_WATER)) aButtonTask = AButtonTask.AIR_DASH
            GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "End method called")
        })

    val swim = Behavior(
        evaluate = {
            if (!ready || !canMove) return@Behavior false

            if (damaged || isBehaviorActive(BehaviorType.RIDING_CART) || !body.isSensing(BodySense.IN_WATER) ||
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)
            ) return@Behavior false

            return@Behavior if (isBehaviorActive(BehaviorType.SWIMMING)) when (directionRotation) {
                Direction.UP -> body.physics.velocity.y > 0f
                Direction.DOWN -> body.physics.velocity.y < 0f
                Direction.LEFT -> body.physics.velocity.x < 0f
                Direction.RIGHT -> body.physics.velocity.x > 0f
            }
            else {
                val aButtonJustPressed = game.controllerPoller.isJustPressed(ControllerButton.A)
                val doSwim = aButtonJustPressed && aButtonTask == AButtonTask.SWIM
                doSwim
            }
        },
        init = {
            body.physics.velocity.add(
                (when (directionRotation) {
                    Direction.UP -> Vector2(0f, swimVel)
                    Direction.DOWN -> Vector2(0f, -swimVel)
                    Direction.LEFT -> Vector2(swimVel, 0f)
                    Direction.RIGHT -> Vector2(-swimVel, 0f)
                }).scl(ConstVals.PPM.toFloat())
            )
            requestToPlaySound(SoundAsset.SWIM_SOUND, false)
            GameLogger.debug(MEGAMAN_SWIM_BEHAVIOR_TAG, "Init method called")
        })

    val jump = Behavior(
        evaluate = {
            if (!ready || !canMove) return@Behavior false

            if (damaged || teleporting || isAnyBehaviorActive(BehaviorType.SWIMMING, BehaviorType.CLIMBING) ||
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) || !game.controllerPoller.isPressed(ControllerButton.A) ||
                game.controllerPoller.isPressed(ControllerButton.DOWN)
            ) return@Behavior false

            return@Behavior if (isBehaviorActive(BehaviorType.JUMPING)) {
                val velocity = body.physics.velocity
                when (directionRotation) {
                    Direction.UP -> velocity.y > 0f
                    Direction.DOWN -> velocity.y < 0f
                    Direction.LEFT -> velocity.x < 0f
                    Direction.RIGHT -> velocity.x > 0f
                }
            } else aButtonTask == AButtonTask.JUMP && game.controllerPoller.isJustPressed(ControllerButton.A) && (body.isSensing(
                BodySense.FEET_ON_GROUND
            ) || isBehaviorActive(BehaviorType.WALL_SLIDING))
        },
        init = {
            val v = Vector2()
            v.x = when (directionRotation) {
                Direction.UP, Direction.DOWN -> {
                    if (isBehaviorActive(BehaviorType.WALL_SLIDING)) MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM * facing.value
                    else body.physics.velocity.x
                }

                Direction.LEFT, Direction.RIGHT -> {
                    ConstVals.PPM * if (isBehaviorActive(BehaviorType.WALL_SLIDING)) wallJumpVel
                    else if (isBehaviorActive(BehaviorType.RIDING_CART)) cartJumpVel
                    else jumpVel
                }
            }
            v.y = when (directionRotation) {
                Direction.UP, Direction.DOWN -> {
                    ConstVals.PPM * if (isBehaviorActive(BehaviorType.WALL_SLIDING)) wallJumpVel
                    else if (isBehaviorActive(BehaviorType.RIDING_CART)) cartJumpVel
                    else jumpVel
                }

                Direction.LEFT, Direction.RIGHT -> {
                    if (isBehaviorActive(BehaviorType.WALL_SLIDING)) MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM * facing.value
                    else body.physics.velocity.y
                }
            }
            body.physics.velocity.set(v)
            requestToPlaySound(SoundAsset.WALL_JUMP_SOUND, false)
            GameLogger.debug(MEGAMAN_JUMP_BEHAVIOR_TAG, "Init method called")
        },
        end = {
            if (isDirectionRotatedVertically()) body.physics.velocity.y = 0f
            else body.physics.velocity.x = 0f
            GameLogger.debug(MEGAMAN_JUMP_BEHAVIOR_TAG, "End method called")
        })

    val airDash = object : AbstractBehavior() {

        private var lastFacing = Facing.RIGHT
        private val impulse = Vector2()

        override fun evaluate(delta: Float): Boolean {
            if (!ready || !canMove) return false

            if (damaged || teleporting || airDashTimer.isFinished() ||
                body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.TELEPORTING) ||
                isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.CLIMBING, BehaviorType.RIDING_CART)
            ) return false

            return if (isBehaviorActive(BehaviorType.AIR_DASHING)) game.controllerPoller.isPressed(ControllerButton.A)
            else game.controllerPoller.isJustPressed(ControllerButton.A) && aButtonTask == AButtonTask.AIR_DASH
        }

        override fun init() {
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "Init")
            body.physics.gravityOn = false
            aButtonTask = AButtonTask.JUMP
            requestToPlaySound(SoundAsset.WHOOSH_SOUND, false)

            if (isDirectionRotatedVertically()) impulse.y = 0f else impulse.x = 0f

            val impulseValue =
                facing.value * ConstVals.PPM * if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_VEL
                else MegamanValues.AIR_DASH_VEL

            when (directionRotation) {
                Direction.UP, Direction.DOWN -> impulse.x = impulseValue

                Direction.LEFT, Direction.RIGHT -> impulse.y = impulseValue
            }

            lastFacing = facing

            putProperty(MegamanKeys.DIRECTION_ON_AIR_DASH, directionRotation)
        }

        override fun act(delta: Float) {
            airDashTimer.update(delta)
            if (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) || isFacing(Facing.RIGHT) && body.isSensing(
                    BodySense.SIDE_TOUCHING_BLOCK_RIGHT
                )
            ) return

            if (facing != lastFacing) impulse.scl(-1f)
            lastFacing = facing

            body.physics.velocity.set(impulse)
        }

        override fun end() {
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "End")
            airDashTimer.reset()
            body.physics.gravityOn = true

            if (!body.isSensing(BodySense.TELEPORTING) && !teleporting) {
                val impulseOnEnd =
                    facing.value * ConstVals.PPM * if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_END_BUMP
                    else MegamanValues.AIR_DASH_END_BUMP

                when (directionRotation) {
                    Direction.UP -> body.physics.velocity.x += impulseOnEnd
                    Direction.DOWN -> body.physics.velocity.x -= impulseOnEnd
                    Direction.LEFT -> body.physics.velocity.y += impulseOnEnd
                    Direction.RIGHT -> body.physics.velocity.y -= impulseOnEnd
                }
            }
        }
    }

    val groundSlide = object : AbstractBehavior() {

        private var directionOnInit: Direction? = null

        override fun evaluate(delta: Float): Boolean {
            if (!ready || !canMove) return false

            if (isBehaviorActive(BehaviorType.GROUND_SLIDING) && body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) return true

            if (damaged || groundSlideTimer.isFinished() || isBehaviorActive(BehaviorType.RIDING_CART) ||
                !body.isSensing(BodySense.FEET_ON_GROUND) || !game.controllerPoller.isPressed(ControllerButton.DOWN)
            ) return false

            return if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) game.controllerPoller.isPressed(ControllerButton.A) && directionOnInit == directionRotation
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

            if (damaged || isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)
            ) return

            val impulse = (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_GROUND_SLIDE_VEL
            else MegamanValues.GROUND_SLIDE_VEL) * ConstVals.PPM * facing.value

            when (directionRotation) {
                Direction.UP, Direction.DOWN -> body.physics.velocity.x = impulse

                Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y = impulse
            }
        }

        override fun end() {
            groundSlideTimer.reset()

            val endDash = (if (body.isSensing(BodySense.IN_WATER)) 2f else 5f) * ConstVals.PPM * facing.value

            if (directionOnInit == directionRotation) {
                when (directionRotation) {
                    Direction.UP, Direction.DOWN -> body.physics.velocity.x += endDash

                    Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y += endDash
                }
            } else {
                body.physics.velocity.setZero()
                when (directionOnInit) {
                    Direction.UP, Direction.DOWN -> body.physics.velocity.x = endDash

                    Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y = endDash

                    null -> throw IllegalStateException("Direction on init cannot be null")
                }
            }

            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "End method called")
        }
    }

    val climb = object : AbstractBehavior() {

        private lateinit var ladder: Ladder

        override fun evaluate(delta: Float): Boolean {
            if (!ready || !canMove) return false

            if (damaged || isAnyBehaviorActive(
                    BehaviorType.JUMPING,
                    BehaviorType.AIR_DASHING,
                    BehaviorType.GROUND_SLIDING,
                    BehaviorType.RIDING_CART
                ) || !body.properties.containsKey(ConstKeys.LADDER)
            ) return false

            ladder = body.properties.get(ConstKeys.LADDER, Ladder::class)!!

            val center = body.getCenter()

            if (isBehaviorActive(BehaviorType.CLIMBING)) {
                if (isDirectionRotatedVertically()) {
                    if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) {
                        if (isDirectionRotatedDown() && center.y + 0.25f * ConstVals.PPM < ladder.body.y) return false
                        else if (center.y - 0.25f * ConstVals.PPM > ladder.body.getMaxY()) return false
                    }

                    if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) {
                        if (isDirectionRotatedDown() && center.y - 0.25f * ConstVals.PPM > ladder.body.getMaxY()) return false
                        else if (center.y + 0.25f * ConstVals.PPM < ladder.body.y) return false
                    }
                } else {
                    if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) {
                        if (isDirectionRotatedLeft() && center.x + 0.25f * ConstVals.PPM < ladder.body.x) return false
                        else if (center.x - 0.25f * ConstVals.PPM > ladder.body.getMaxX()) return false
                    }

                    if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) {
                        if (isDirectionRotatedLeft() && center.x + 0.25f * ConstVals.PPM > ladder.body.getMaxX()) return false
                        else if (center.x - 0.25f * ConstVals.PPM < ladder.body.x) return false
                    }
                }

                if (game.controllerPoller.isJustPressed(ControllerButton.A)) return false

                return true
            }

            if (body.isSensing(BodySense.FEET_TOUCHING_LADDER) && game.controllerPoller.isPressed(
                    ControllerButton.DOWN
                )
            ) return true

            if (body.isSensing(BodySense.HEAD_TOUCHING_LADDER) && game.controllerPoller.isPressed(
                    ControllerButton.UP
                )
            ) return true

            return false
        }

        override fun init() {
            aButtonTask = if (body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM else AButtonTask.AIR_DASH
            body.physics.gravityOn = false

            when (directionRotation) {
                Direction.UP, Direction.DOWN -> {
                    body.setCenterX(ladder.body.getCenter().x)

                    if (body.getMaxY() <= ladder.body.y) body.setY(ladder.body.y)
                    else if (body.y >= ladder.body.getMaxY()) body.setMaxY(ladder.body.getMaxY())
                }

                Direction.LEFT, Direction.RIGHT -> {
                    body.setCenterY(ladder.body.getCenter().y)

                    if (body.getMaxX() <= ladder.body.x) body.setX(ladder.body.x)
                    else if (body.x >= ladder.body.getMaxX()) body.setMaxX(ladder.body.getMaxX())
                }
            }
            body.physics.velocity.setZero()
        }

        override fun act(delta: Float) {
            when (directionRotation) {
                Direction.UP, Direction.DOWN -> body.setCenterX(ladder.body.getCenter().x)

                Direction.LEFT, Direction.RIGHT -> body.setCenterY(ladder.body.getCenter().y)
            }
            if (shooting) {
                body.physics.velocity.setZero()
                return
            }
            body.physics.velocity = (when (directionRotation) {
                Direction.UP -> if (game.controllerPoller.isPressed(ControllerButton.UP)) Vector2(
                    0f, MegamanValues.CLIMB_VEL
                )
                else if (game.controllerPoller.isPressed(ControllerButton.DOWN)) Vector2(
                    0f, MegamanValues.CLIMB_VEL * -1f
                )
                else Vector2()

                Direction.DOWN -> if (game.controllerPoller.isPressed(ControllerButton.DOWN)) Vector2(
                    0f, MegamanValues.CLIMB_VEL
                )
                else if (game.controllerPoller.isPressed(ControllerButton.UP)) Vector2(
                    0f, MegamanValues.CLIMB_VEL * -1f
                )
                else Vector2()

                Direction.LEFT -> {
                    if (game.controllerPoller.isPressed(ControllerButton.UP)) Vector2(
                        MegamanValues.CLIMB_VEL * -1f, 0f
                    )
                    else if (game.controllerPoller.isPressed(ControllerButton.DOWN)) Vector2(
                        MegamanValues.CLIMB_VEL, 0f
                    )
                    else Vector2()
                }

                Direction.RIGHT -> {
                    if (game.controllerPoller.isPressed(ControllerButton.UP)) Vector2(
                        MegamanValues.CLIMB_VEL, 0f
                    )
                    else if (game.controllerPoller.isPressed(ControllerButton.DOWN)) Vector2(
                        MegamanValues.CLIMB_VEL * -1f, 0f
                    )
                    else Vector2()
                }
            }).scl(ConstVals.PPM.toFloat())
        }

        override fun end() {
            body.physics.gravityOn = true
            body.physics.velocity.setZero()
            aButtonTask = if (body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM else AButtonTask.AIR_DASH
        }
    }

    val ridingCart = object : AbstractBehavior() {

        private lateinit var cart: Cart

        override fun evaluate(delta: Float) =
            ready && canMove &&
                    body.isSensing(BodySense.TOUCHING_CART) &&
                    !game.controllerPoller.areAllPressed(
                        gdxArrayOf(ControllerButton.A, ControllerButton.UP)
                    )

        override fun init() {
            body.physics.velocity.setZero()
            aButtonTask = AButtonTask.JUMP

            cart = body.getProperty(ConstKeys.CART, Cart::class)!!
            cart.body.physics.gravityOn = false

            cart.childBlock.body.physics.collisionOn = false
            cart.childBlock.body.fixtures.forEach { (it.second as Fixture).active = false }

            body.setBottomCenterToPoint(cart.body.getBottomCenterPoint())
            body.preProcess.put(ConstKeys.CART, Updatable {
                cart.body.setCenter(body.getCenter())
            })

            cart.sprites.values().forEach { it.hidden = true }
        }

        override fun act(delta: Float) {
            body.physics.velocity.x =
                if ((body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && isFacing(Facing.RIGHT)) || (body.isSensing(
                        BodySense.SIDE_TOUCHING_BLOCK_LEFT
                    ) && isFacing(Facing.LEFT))
                ) 0f else MegamanValues.CART_RIDE_MAX_SPEED * ConstVals.PPM * facing.value
        }

        override fun end() {
            cart.body.physics.gravityOn = true
            cart.body.physics.velocity.x = body.physics.velocity.x
            cart.body.physics.velocity.y = 0f

            cart.sprites.values().forEach { it.hidden = false }

            cart.childBlock.body.physics.collisionOn = true
            cart.childBlock.body.fixtures.forEach { (it.second as Fixture).active = true }

            body.translation(0f, ConstVals.PPM / 1.75f)
            body.physics.velocity.y = MegamanValues.JUMP_VEL * ConstVals.PPM
            body.preProcess.remove(ConstKeys.CART)
        }
    }

    behaviorsComponent.addBehavior(BehaviorType.WALL_SLIDING, wallSlide)
    behaviorsComponent.addBehavior(BehaviorType.SWIMMING, swim)
    behaviorsComponent.addBehavior(BehaviorType.JUMPING, jump)
    behaviorsComponent.addBehavior(BehaviorType.AIR_DASHING, airDash)
    behaviorsComponent.addBehavior(BehaviorType.GROUND_SLIDING, groundSlide)
    behaviorsComponent.addBehavior(BehaviorType.CLIMBING, climb)
    behaviorsComponent.addBehavior(BehaviorType.RIDING_CART, ridingCart)

    return behaviorsComponent
}
