package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.behaviors.AbstractBehaviorImpl
import com.mega.game.engine.behaviors.BehaviorsComponent
import com.mega.game.engine.behaviors.FunctionalBehaviorImpl
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf

import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.controller.buttons.ButtonStatus
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButtons
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.special.Cart
import com.megaman.maverick.game.entities.special.Ladder
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.world.body.isSensingAny

const val MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: WallSlideBehavior"
const val MEGAMAN_SWIM_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: SwimBehavior"
const val MEGAMAN_JUMP_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: JumpBehavior"
const val MEGAMAN_AIR_DASH_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: AirDashBehavior"
const val MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: GroundSlideBehavior"
const val MEGAMAN_CLIMB_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: ClimbBehavior"

internal fun Megaman.defineBehaviorsComponent(): BehaviorsComponent {
    val behaviorsComponent = BehaviorsComponent()

    val wallSlide = FunctionalBehaviorImpl(
        evaluate = {
            if (dead || !ready || !canMove || !has(MegaAbility.WALL_SLIDE) || body.isSensing(BodySense.FEET_ON_SAND) ||
                isAnyBehaviorActive(BehaviorType.JETPACKING, BehaviorType.RIDING_CART)
            ) return@FunctionalBehaviorImpl false

            if ((body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && game.controllerPoller.isPressed(
                    /* if (isDirectionRotatedDown() || isDirectionRotatedRight()) MegaControllerButtons.RIGHT
                    else */ MegaControllerButtons.LEFT
                )) || body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && game.controllerPoller.isPressed(
                    /* if (isDirectionRotatedDown() || isDirectionRotatedRight()) MegaControllerButtons.LEFT
                    else */ MegaControllerButtons.RIGHT
                )
            ) {
                if (damaged) {
                    GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Damaged")
                    return@FunctionalBehaviorImpl false
                }
                if (isBehaviorActive(BehaviorType.JUMPING)) {
                    GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Jumping")
                    return@FunctionalBehaviorImpl false
                }
                if (isBehaviorActive(BehaviorType.CLIMBING)) {
                    GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Climbing")
                    return@FunctionalBehaviorImpl false
                }
                if (isBehaviorActive(BehaviorType.RIDING_CART)) {
                    GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Riding cart")
                    return@FunctionalBehaviorImpl false
                }
                if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                    GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Feet on ground")
                    return@FunctionalBehaviorImpl false
                }
                if (!wallJumpTimer.isFinished()) {
                    GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Wall jump timer not finished")
                    return@FunctionalBehaviorImpl false
                }
                return@FunctionalBehaviorImpl true
            } else return@FunctionalBehaviorImpl false
        },
        init = {
            aButtonTask = AButtonTask.JUMP
            GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "Init method called")
        },
        act = {
            aButtonTask = AButtonTask.JUMP
            if (isDirectionRotatedVertically()) body.physics.frictionOnSelf.y += MegamanValues.WALL_SLIDE_FRICTION_TO_APPLY
            else body.physics.frictionOnSelf.x += MegamanValues.WALL_SLIDE_FRICTION_TO_APPLY
        },
        end = {
            if (!body.isSensing(BodySense.IN_WATER)) aButtonTask = AButtonTask.AIR_DASH
            GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "End method called")
        })

    val swim = FunctionalBehaviorImpl(
        evaluate = {
            if (dead || !ready || !canMove) return@FunctionalBehaviorImpl false

            if (damaged || isBehaviorActive(BehaviorType.RIDING_CART) || !body.isSensing(BodySense.IN_WATER) ||
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)
            ) return@FunctionalBehaviorImpl false

            return@FunctionalBehaviorImpl if (isBehaviorActive(BehaviorType.SWIMMING)) when (directionRotation!!) {
                Direction.UP -> body.physics.velocity.y > 0f
                Direction.DOWN -> body.physics.velocity.y < 0f
                Direction.LEFT -> body.physics.velocity.x < 0f
                Direction.RIGHT -> body.physics.velocity.x > 0f
            }
            else {
                val aButtonJustPressed = game.controllerPoller.isJustPressed(MegaControllerButtons.A)
                val doSwim = aButtonJustPressed && aButtonTask == AButtonTask.SWIM
                doSwim
            }
        },
        init = {
            body.physics.velocity.add(
                (when (directionRotation!!) {
                    Direction.UP -> Vector2(0f, swimVel)
                    Direction.DOWN -> Vector2(0f, -swimVel)
                    Direction.LEFT -> Vector2(swimVel, 0f)
                    Direction.RIGHT -> Vector2(-swimVel, 0f)
                }).scl(ConstVals.PPM.toFloat())
            )
            requestToPlaySound(SoundAsset.SWIM_SOUND, false)
            GameLogger.debug(MEGAMAN_SWIM_BEHAVIOR_TAG, "Init method called")
        })

    val jump = FunctionalBehaviorImpl(
        evaluate = {
            if (dead || !ready || !canMove || damaged || teleporting || isAnyBehaviorActive(
                    BehaviorType.SWIMMING, BehaviorType.CLIMBING, BehaviorType.JETPACKING
                ) || body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) ||
                !game.controllerPoller.isPressed(MegaControllerButtons.A) ||
                game.controllerPoller.isPressed(MegaControllerButtons.DOWN)
            ) return@FunctionalBehaviorImpl false

            return@FunctionalBehaviorImpl if (isBehaviorActive(BehaviorType.JUMPING)) {
                val velocity = body.physics.velocity
                when (directionRotation!!) {
                    Direction.UP -> velocity.y > 0f
                    Direction.DOWN -> velocity.y < 0f
                    Direction.LEFT -> velocity.x < 0f
                    Direction.RIGHT -> velocity.x > 0f
                }
            } else aButtonTask == AButtonTask.JUMP && game.controllerPoller.isJustPressed(MegaControllerButtons.A) &&
                    (body.isSensing(BodySense.FEET_ON_GROUND) || isBehaviorActive(BehaviorType.WALL_SLIDING))
        },
        init = {
            val v = Vector2()
            v.x = when (directionRotation!!) {
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
            v.y = when (directionRotation!!) {
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

    val airDash = object : AbstractBehaviorImpl() {

        private var lastFacing = Facing.RIGHT
        private val impulse = Vector2()

        override fun evaluate(delta: Float): Boolean {
            if (dead || !ready || !canMove || damaged || teleporting || airDashTimer.isFinished() ||
                !has(MegaAbility.AIR_DASH) || body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.TELEPORTING) ||
                isAnyBehaviorActive(
                    BehaviorType.WALL_SLIDING, BehaviorType.CLIMBING, BehaviorType.RIDING_CART,
                    BehaviorType.JETPACKING
                )
            ) return false

            return if (isBehaviorActive(BehaviorType.AIR_DASHING))
                game.controllerPoller.isPressed(MegaControllerButtons.A)
            else aButtonTask == AButtonTask.AIR_DASH &&
                    game.controllerPoller.isJustPressed(MegaControllerButtons.A) &&
                    game.controllerPoller.isReleased(MegaControllerButtons.DOWN) &&
                    (if (currentWeapon == MegamanWeapon.RUSH_JETPACK)
                        game.controllerPoller.isReleased(MegaControllerButtons.UP) else true)
        }

        override fun init() {
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "Init")
            body.physics.gravityOn = false
            aButtonTask = AButtonTask.JUMP
            requestToPlaySound(SoundAsset.WHOOSH_SOUND, false)

            if (isDirectionRotatedVertically()) impulse.y = 0f else impulse.x = 0f

            val impulseValue = facing.value * ConstVals.PPM * movementScalar *
                    (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_VEL else MegamanValues.AIR_DASH_VEL)
            when (directionRotation!!) {
                Direction.UP, Direction.DOWN -> impulse.x = impulseValue

                Direction.LEFT, Direction.RIGHT -> impulse.y = impulseValue
            }

            lastFacing = facing
            putProperty(MegamanKeys.DIRECTION_ON_AIR_DASH, directionRotation)
        }

        override fun act(delta: Float) {
            airDashTimer.update(delta)
            if (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)
            ) return

            if (facing != lastFacing) impulse.scl(-1f)
            lastFacing = facing

            body.physics.velocity.set(impulse)
        }

        override fun end() {
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "End")
            airDashTimer.reset()
            if (!cameraRotating) {
                body.physics.gravityOn = true
                if (!body.isSensing(BodySense.TELEPORTING) && !teleporting) {
                    val impulseOnEnd =
                        facing.value * ConstVals.PPM * if (body.isSensing(BodySense.IN_WATER))
                            MegamanValues.WATER_AIR_DASH_END_BUMP else MegamanValues.AIR_DASH_END_BUMP

                    when (directionRotation!!) {
                        Direction.UP -> body.physics.velocity.x += impulseOnEnd
                        Direction.DOWN -> body.physics.velocity.x -= impulseOnEnd
                        Direction.LEFT -> body.physics.velocity.y += impulseOnEnd
                        Direction.RIGHT -> body.physics.velocity.y -= impulseOnEnd
                    }
                }
            }
        }
    }

    val groundSlide = object : AbstractBehaviorImpl() {

        private var directionOnInit: Direction? = null

        override fun evaluate(delta: Float): Boolean {
            if (dead || !ready || !canMove || cameraRotating || !has(MegaAbility.GROUND_SLIDE) ||
                body.isSensing(BodySense.FEET_ON_SAND)
            ) return false

            if (isBehaviorActive(BehaviorType.GROUND_SLIDING) && body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))
                return true

            if (damaged || groundSlideTimer.isFinished() || isAnyBehaviorActive(
                    BehaviorType.RIDING_CART,
                    BehaviorType.JETPACKING
                ) || !body.isSensing(BodySense.FEET_ON_GROUND) ||
                !game.controllerPoller.isPressed(MegaControllerButtons.DOWN)
            ) return false

            return if (isBehaviorActive(BehaviorType.GROUND_SLIDING))
                game.controllerPoller.isPressed(MegaControllerButtons.A) && directionOnInit == directionRotation
            else game.controllerPoller.isJustPressed(MegaControllerButtons.A)
        }

        override fun init() {
            // In body pre-process, body height is reduced from .95f to .45f when ground sliding;
            // when upside down, need to compensate, otherwise Megaman will be off the ground
            when (directionRotation!!) {
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
            else MegamanValues.GROUND_SLIDE_VEL) * ConstVals.PPM * facing.value * movementScalar

            when (directionRotation!!) {
                Direction.UP, Direction.DOWN -> body.physics.velocity.x = impulse
                Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y = impulse
            }
        }

        override fun end() {
            groundSlideTimer.reset()

            if (!cameraRotating) {
                val endDash = (if (body.isSensing(BodySense.IN_WATER)) 2f else 5f) * ConstVals.PPM * facing.value

                if (directionOnInit == directionRotation) when (directionRotation!!) {
                    Direction.UP, Direction.DOWN -> body.physics.velocity.x += endDash
                    Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y += endDash
                } else {
                    body.physics.velocity.setZero()
                    when (directionOnInit) {
                        Direction.UP, Direction.DOWN -> body.physics.velocity.x = endDash
                        Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y = endDash
                        null -> throw IllegalStateException("Direction on init cannot be null")
                    }
                }
            }

            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "End method called")
        }
    }

    val climb = object : AbstractBehaviorImpl() {

        private lateinit var ladder: Ladder

        override fun evaluate(delta: Float): Boolean {
            if (dead || !ready || !canMove) return false

            if (damaged || isAnyBehaviorActive(
                    BehaviorType.JUMPING,
                    BehaviorType.AIR_DASHING,
                    BehaviorType.GROUND_SLIDING,
                    BehaviorType.RIDING_CART,
                    BehaviorType.SWIMMING,
                    BehaviorType.JETPACKING
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

                if (game.controllerPoller.isJustPressed(MegaControllerButtons.A)) return false

                return true
            }

            if (body.isSensing(BodySense.FEET_TOUCHING_LADDER) &&
                game.controllerPoller.isPressed(MegaControllerButtons.DOWN)
            ) return true

            if (body.isSensing(BodySense.HEAD_TOUCHING_LADDER) &&
                game.controllerPoller.isPressed(MegaControllerButtons.UP)
            ) return true

            return false
        }

        override fun init() {
            aButtonTask = if (body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM else AButtonTask.AIR_DASH
            body.physics.gravityOn = false

            when (directionRotation!!) {
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
            when (directionRotation!!) {
                Direction.UP, Direction.DOWN -> body.setCenterX(ladder.body.getCenter().x)

                Direction.LEFT, Direction.RIGHT -> body.setCenterY(ladder.body.getCenter().y)
            }

            if (shooting || game.isProperty(ConstKeys.ROOM_TRANSITION, true)) {
                GameLogger.debug(MEGAMAN_CLIMB_BEHAVIOR_TAG, "Shooting or room transition")
                body.physics.velocity.setZero()
                return
            }

            body.physics.velocity = (when (directionRotation!!) {
                Direction.UP -> if (game.controllerPoller.isPressed(MegaControllerButtons.UP))
                    Vector2(0f, MegamanValues.CLIMB_VEL)
                else if (game.controllerPoller.isPressed(MegaControllerButtons.DOWN))
                    Vector2(0f, MegamanValues.CLIMB_VEL * -1f)
                else Vector2()

                Direction.DOWN -> if (game.controllerPoller.isPressed(MegaControllerButtons.DOWN))
                    Vector2(0f, MegamanValues.CLIMB_VEL)
                else if (game.controllerPoller.isPressed(MegaControllerButtons.UP))
                    Vector2(0f, MegamanValues.CLIMB_VEL * -1f)
                else Vector2()

                Direction.LEFT -> {
                    if (game.controllerPoller.isPressed(MegaControllerButtons.UP))
                        Vector2(MegamanValues.CLIMB_VEL * -1f, 0f)
                    else if (game.controllerPoller.isPressed(MegaControllerButtons.DOWN))
                        Vector2(MegamanValues.CLIMB_VEL, 0f)
                    else Vector2()
                }

                Direction.RIGHT -> {
                    if (game.controllerPoller.isPressed(MegaControllerButtons.UP))
                        Vector2(MegamanValues.CLIMB_VEL, 0f)
                    else if (game.controllerPoller.isPressed(MegaControllerButtons.DOWN))
                        Vector2(MegamanValues.CLIMB_VEL * -1f, 0f)
                    else Vector2()
                }
            }).scl(ConstVals.PPM * movementScalar)
        }

        override fun end() {
            body.physics.gravityOn = true
            body.physics.velocity.setZero()
            aButtonTask = if (body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM else AButtonTask.AIR_DASH
        }
    }

    val ridingCart = object : AbstractBehaviorImpl() {

        private lateinit var cart: Cart

        override fun evaluate(delta: Float) =
            !dead && ready && body.isSensing(BodySense.TOUCHING_CART) &&
                    !game.controllerPoller.areAllPressed(gdxArrayOf(MegaControllerButtons.A, MegaControllerButtons.UP))

        override fun init() {
            body.physics.velocity.setZero()
            aButtonTask = AButtonTask.JUMP

            cart = body.getProperty(ConstKeys.CART, Cart::class)!!
            // cart.body.physics.gravityOn = false
            cart.childBlock!!.body.physics.collisionOn = false
            cart.childBlock!!.body.fixtures.forEach { (it.second as Fixture).active = false }

            body.setBottomCenterToPoint(cart.body.getBottomCenterPoint())
            body.preProcess.put(ConstKeys.CART) { cart.body.setCenter(body.getCenter()) }

            cart.sprites.values().forEach { it.hidden = true }

            gdxArrayOf(feetFixture, leftSideFixture, rightSideFixture, bodyFixture).forEach { fixture ->
                fixture.putProperty(ConstKeys.DEATH_LISTENER, false)
            }
            requestToPlaySound(SoundAsset.CONVEYOR_LIFT_SOUND, true)
        }

        override fun act(delta: Float) {
            if ((body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && isFacing(Facing.RIGHT)) ||
                (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && isFacing(Facing.LEFT))
            ) body.physics.velocity.x = 0f
            else if (!body.isSensing(BodySense.FEET_ON_GROUND))
                body.physics.velocity.x += MegamanValues.CART_JUMP_ACCELERATION * facing.value * ConstVals.PPM
            else body.physics.velocity.x += MegamanValues.CART_RIDE_ACCELERATION * facing.value * ConstVals.PPM
        }

        override fun end() {
            cart.body.physics.gravityOn = true
            cart.body.physics.velocity.x = body.physics.velocity.x
            cart.body.physics.velocity.y = 0f

            cart.sprites.values().forEach { it.hidden = false }

            cart.childBlock!!.body.physics.collisionOn = true
            cart.childBlock!!.body.fixtures.forEach { (it.second as Fixture).active = true }

            if (!dead) {
                body.translation(0f, ConstVals.PPM / 1.75f)
                body.physics.velocity.y = MegamanValues.JUMP_VEL * ConstVals.PPM
            }
            body.preProcess.remove(ConstKeys.CART)

            gdxArrayOf(feetFixture, leftSideFixture, rightSideFixture, bodyFixture).forEach { fixture ->
                fixture.putProperty(ConstKeys.DEATH_LISTENER, true)
            }

            stopSoundNow(SoundAsset.CONVEYOR_LIFT_SOUND)
            aButtonTask = AButtonTask.AIR_DASH
        }
    }

    val jetpacking = object : AbstractBehaviorImpl() {

        private val timePerBitTimer = Timer(MegamanValues.JETPACK_TIME_PER_BIT)

        override fun evaluate(delta: Float): Boolean {
            if (dead || !ready || !canMove || damaged || teleporting || currentWeapon != MegamanWeapon.RUSH_JETPACK ||
                !game.controllerPoller.areAllPressed(gdxArrayOf(MegaControllerButtons.A, MegaControllerButtons.UP)) ||
                body.isSensing(BodySense.FEET_ON_GROUND) || weaponHandler.isDepleted(MegamanWeapon.RUSH_JETPACK) ||
                isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.AIR_DASHING, BehaviorType.GROUND_SLIDING)
            ) return false

            return if (isBehaviorActive(BehaviorType.JETPACKING)) game.controllerPoller.areAllPressed(
                gdxArrayOf(MegaControllerButtons.A, MegaControllerButtons.UP)
            )
            else game.controllerPoller.allMatch(
                objectMapOf(
                    MegaControllerButtons.UP to ButtonStatus.PRESSED,
                    MegaControllerButtons.A to ButtonStatus.JUST_PRESSED
                )
            )
        }

        override fun init() {
            requestToPlaySound(SoundAsset.JETPACK_SOUND, true)
            body.physics.gravityOn = false
            timePerBitTimer.reset()
        }

        override fun act(delta: Float) {
            when (directionRotation!!) {
                Direction.UP -> body.physics.velocity.y =
                    MegamanValues.JETPACK_Y_IMPULSE * ConstVals.PPM * movementScalar

                Direction.DOWN -> body.physics.velocity.y =
                    -MegamanValues.JETPACK_Y_IMPULSE * ConstVals.PPM * movementScalar

                Direction.LEFT -> body.physics.velocity.x =
                    -MegamanValues.JETPACK_Y_IMPULSE * ConstVals.PPM * movementScalar

                Direction.RIGHT -> body.physics.velocity.x =
                    MegamanValues.JETPACK_Y_IMPULSE * ConstVals.PPM * movementScalar
            }
            timePerBitTimer.update(delta)
            if (timePerBitTimer.isFinished()) {
                weaponHandler.translateAmmo(MegamanWeapon.RUSH_JETPACK, -1)
                timePerBitTimer.reset()
            }
        }

        override fun end() {
            body.physics.gravityOn = true
            stopSoundNow(SoundAsset.JETPACK_SOUND)
        }
    }

    behaviorsComponent.addBehavior(BehaviorType.WALL_SLIDING, wallSlide)
    behaviorsComponent.addBehavior(BehaviorType.SWIMMING, swim)
    behaviorsComponent.addBehavior(BehaviorType.JUMPING, jump)
    behaviorsComponent.addBehavior(BehaviorType.AIR_DASHING, airDash)
    behaviorsComponent.addBehavior(BehaviorType.GROUND_SLIDING, groundSlide)
    behaviorsComponent.addBehavior(BehaviorType.CLIMBING, climb)
    behaviorsComponent.addBehavior(BehaviorType.RIDING_CART, ridingCart)
    behaviorsComponent.addBehavior(BehaviorType.JETPACKING, jetpacking)

    return behaviorsComponent
}
