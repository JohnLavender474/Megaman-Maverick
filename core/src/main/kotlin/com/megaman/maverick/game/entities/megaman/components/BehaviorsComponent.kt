package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.behaviors.AbstractBehaviorImpl
import com.mega.game.engine.behaviors.BehaviorsComponent
import com.mega.game.engine.behaviors.FunctionalBehaviorImpl
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.controller.buttons.ButtonStatus
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.special.Cart
import com.megaman.maverick.game.entities.special.Ladder
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

const val MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: WallSlideBehavior"
const val MEGAMAN_SWIM_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: SwimBehavior"
const val MEGAMAN_JUMP_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: JumpBehavior"
const val MEGAMAN_AIR_DASH_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: AirDashBehavior"
const val MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: GroundSlideBehavior"
const val MEGAMAN_CLIMB_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: ClimbBehavior"

const val MEGAMAN_LADDER_MOVE_OFFSET = 0.5f

internal fun Megaman.defineBehaviorsComponent(): BehaviorsComponent {
    val behaviorsComponent = BehaviorsComponent()

    val wallSlide = FunctionalBehaviorImpl(
        evaluate = {
            if (dead || !ready || !canMove || !has(MegaAbility.WALL_SLIDE) || body.isSensing(BodySense.FEET_ON_SAND) ||
                isAnyBehaviorActive(BehaviorType.JETPACKING, BehaviorType.RIDING_CART)
            ) return@FunctionalBehaviorImpl false

            if ((body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && game.controllerPoller.isPressed(
                    /* if (direction == Direction.DOWN || isDirectionRotatedRight()) MegaControllerButtons.RIGHT
                    else */ MegaControllerButton.LEFT
                )) || body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && game.controllerPoller.isPressed(
                    /* if (direction == Direction.DOWN || isDirectionRotatedRight()) MegaControllerButtons.LEFT
                    else */ MegaControllerButton.RIGHT
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
            // TODO: this logic is tied to the FPS and breaks as it deviates from 60 FPS
            //  an alternative that is FPS-agnostic needs to be found
            val wallSlideFriction = MegamanValues.WALL_SLIDE_FRICTION_TO_APPLY * ConstVals.PPM
            if (direction.isVertical()) body.physics.frictionOnSelf.y = wallSlideFriction
            else body.physics.frictionOnSelf.x = wallSlideFriction
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

            return@FunctionalBehaviorImpl if (isBehaviorActive(BehaviorType.SWIMMING)) when (direction) {
                Direction.UP -> body.physics.velocity.y > 0f
                Direction.DOWN -> body.physics.velocity.y < 0f
                Direction.LEFT -> body.physics.velocity.x < 0f
                Direction.RIGHT -> body.physics.velocity.x > 0f
            } else {
                val aButtonJustPressed = game.controllerPoller.isJustPressed(MegaControllerButton.A)
                val doSwim = aButtonJustPressed && aButtonTask == AButtonTask.SWIM
                doSwim
            }
        },
        init = {
            val impulse = GameObjectPools.fetch(Vector2::class)
            when (direction) {
                Direction.UP -> impulse.set(0f, swimVel)
                Direction.DOWN -> impulse.set(0f, -swimVel)
                Direction.LEFT -> impulse.set(swimVel, 0f)
                Direction.RIGHT -> impulse.set(-swimVel, 0f)
            }.scl(ConstVals.PPM.toFloat())
            if (has(MegaEnhancement.JUMP_BOOST)) impulse.scl(MegaEnhancement.JUMP_BOOST_SCALAR)
            body.physics.velocity.add(impulse)
            // add instead of set because when Megaman jumps, he's grounded, but when he swims, the applied gravity
            // should weigh down his swimming impulse

            requestToPlaySound(SoundAsset.SWIM_SOUND, false)

            GameLogger.debug(MEGAMAN_SWIM_BEHAVIOR_TAG, "Init method called")
        })

    val jump = FunctionalBehaviorImpl(
        evaluate = {
            if (dead || !ready || !canMove || damaged || teleporting ||
                isAnyBehaviorActive(BehaviorType.SWIMMING, BehaviorType.CLIMBING, BehaviorType.JETPACKING) ||
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) ||
                !game.controllerPoller.isPressed(MegaControllerButton.A) ||
                game.controllerPoller.isPressed(MegaControllerButton.DOWN)
            ) return@FunctionalBehaviorImpl false

            return@FunctionalBehaviorImpl if (isBehaviorActive(BehaviorType.JUMPING)) {
                val velocity = body.physics.velocity
                when (direction) {
                    Direction.UP -> velocity.y > 0f
                    Direction.DOWN -> velocity.y < 0f
                    Direction.LEFT -> velocity.x < 0f
                    Direction.RIGHT -> velocity.x > 0f
                }
            } else aButtonTask == AButtonTask.JUMP && game.controllerPoller.isJustPressed(MegaControllerButton.A) &&
                (body.isSensing(BodySense.FEET_ON_GROUND) || isBehaviorActive(BehaviorType.WALL_SLIDING))
        },
        init = {
            val v = GameObjectPools.fetch(Vector2::class)
            v.x = when (direction) {
                Direction.UP, Direction.DOWN -> {
                    when {
                        isBehaviorActive(BehaviorType.WALL_SLIDING) ||
                            (body.isSensingAny(
                                BodySense.SIDE_TOUCHING_BLOCK_LEFT,
                                BodySense.SIDE_TOUCHING_BLOCK_RIGHT
                            ) && !body.isSensing(BodySense.FEET_ON_GROUND)) ->
                            MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM * facing.value

                        else -> body.physics.velocity.x
                    }
                }

                Direction.LEFT, Direction.RIGHT -> {
                    ConstVals.PPM * when {
                        isBehaviorActive(BehaviorType.WALL_SLIDING) -> wallJumpVel
                        isBehaviorActive(BehaviorType.RIDING_CART) -> cartJumpVel
                        has(MegaEnhancement.JUMP_BOOST) -> jumpVel * MegaEnhancement.JUMP_BOOST_SCALAR
                        else -> jumpVel
                    }
                }
            }
            v.y = when (direction) {
                Direction.UP, Direction.DOWN -> {
                    ConstVals.PPM * when {
                        isBehaviorActive(BehaviorType.WALL_SLIDING) -> wallJumpVel
                        isBehaviorActive(BehaviorType.RIDING_CART) -> cartJumpVel
                        has(MegaEnhancement.JUMP_BOOST) -> jumpVel * MegaEnhancement.JUMP_BOOST_SCALAR
                        else -> jumpVel
                    }
                }

                Direction.LEFT, Direction.RIGHT -> {
                    when {
                        isBehaviorActive(BehaviorType.WALL_SLIDING) ||
                            (body.isSensingAny(
                                BodySense.SIDE_TOUCHING_BLOCK_LEFT,
                                BodySense.SIDE_TOUCHING_BLOCK_RIGHT
                            ) && !body.isSensing(BodySense.FEET_ON_GROUND)) ->
                            MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM * facing.value

                        else -> body.physics.velocity.y
                    }
                }
            }
            body.physics.velocity.set(v)

            canMakeLandSound = true

            GameLogger.debug(MEGAMAN_JUMP_BEHAVIOR_TAG, "init(): velocity=$v")
        },
        end = {
            when {
                direction.isVertical() -> body.physics.velocity.y = 0f
                else -> body.physics.velocity.x = 0f
            }

            GameLogger.debug(MEGAMAN_JUMP_BEHAVIOR_TAG, "end(): velocity=${body.physics.velocity}")
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
                game.controllerPoller.isPressed(MegaControllerButton.A)
            else aButtonTask == AButtonTask.AIR_DASH &&
                game.controllerPoller.isJustPressed(MegaControllerButton.A) &&
                game.controllerPoller.isReleased(MegaControllerButton.DOWN) &&
                (if (currentWeapon == MegamanWeapon.RUSH_JETPACK)
                    game.controllerPoller.isReleased(MegaControllerButton.UP) else true)
        }

        override fun init() {
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "init()")
            body.physics.gravityOn = false
            aButtonTask = AButtonTask.JUMP

            if (direction.isVertical()) impulse.y = 0f else impulse.x = 0f

            var impulseValue = facing.value * ConstVals.PPM * movementScalar *
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_VEL else MegamanValues.AIR_DASH_VEL)
            if (has(MegaEnhancement.AIR_DASH_BOOST)) impulseValue *= MegaEnhancement.AIR_DASH_BOOST_SCALAR

            when (direction) {
                Direction.UP, Direction.DOWN -> impulse.x = impulseValue

                Direction.LEFT, Direction.RIGHT -> impulse.y = impulseValue
            }

            lastFacing = facing
            putProperty(MegamanKeys.DIRECTION_ON_AIR_DASH, direction)
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
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "end()")
            airDashTimer.reset()
            if (!game.isCameraRotating()) {
                body.physics.gravityOn = true
                if (!body.isSensing(BodySense.TELEPORTING) && !teleporting) {
                    val impulseOnEnd =
                        facing.value * ConstVals.PPM * if (body.isSensing(BodySense.IN_WATER))
                            MegamanValues.WATER_AIR_DASH_END_BUMP else MegamanValues.AIR_DASH_END_BUMP

                    when (direction) {
                        Direction.UP -> body.physics.velocity.x += impulseOnEnd
                        Direction.DOWN -> body.physics.velocity.x -= impulseOnEnd
                        Direction.LEFT -> body.physics.velocity.y += impulseOnEnd
                        Direction.RIGHT -> body.physics.velocity.y -= impulseOnEnd
                    }
                }
            }
        }
    }

    val crouch = object : AbstractBehaviorImpl() {

        private val timer = Timer(MegamanValues.CROUCH_DELAY)

        override fun evaluate(delta: Float): Boolean {
            if (!has(MegaAbility.CROUCH)) {
                timer.reset()
                return false
            }

            // always update the timer if Megaman has the crouch ability, even when the `if` below returns false
            when {
                !game.controllerPoller.isPressed(MegaControllerButton.DOWN) ||
                    isBehaviorActive(BehaviorType.GROUND_SLIDING) -> timer.reset()

                else -> timer.update(delta)
            }

            if (dead || damaged || !ready || !canMove || slipSliding || game.isCameraRotating() ||
                isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.RIDING_CART, BehaviorType.JETPACKING) ||
                !body.isSensing(BodySense.FEET_ON_GROUND) || body.isSensing(BodySense.FEET_ON_SAND) ||
                !game.controllerPoller.isPressed(MegaControllerButton.DOWN)
            ) return false

            return timer.isFinished()
        }

        override fun act(delta: Float) {
            body.physics.velocity.x = 0f
        }
    }

    val groundSlide = object : AbstractBehaviorImpl() {

        private var directionOnInit: Direction? = null

        override fun evaluate(delta: Float): Boolean {
            if (dead || !ready || !canMove || game.isCameraRotating() || !has(MegaAbility.GROUND_SLIDE) ||
                body.isSensing(BodySense.FEET_ON_SAND)
            ) return false

            if (isBehaviorActive(BehaviorType.GROUND_SLIDING) && body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))
                return true

            if (damaged || groundSlideTimer.isFinished() ||
                isAnyBehaviorActive(BehaviorType.RIDING_CART, BehaviorType.JETPACKING) ||
                !body.isSensing(BodySense.FEET_ON_GROUND) ||
                !game.controllerPoller.isPressed(MegaControllerButton.DOWN)
            ) return false

            return if (isBehaviorActive(BehaviorType.GROUND_SLIDING))
                game.controllerPoller.isPressed(MegaControllerButton.A) && directionOnInit == direction
            else game.controllerPoller.isJustPressed(MegaControllerButton.A)
        }

        override fun init() {
            when (direction) {
                Direction.UP -> {}
                Direction.DOWN -> body.translate(0f, ConstVals.PPM / 2f)
                Direction.LEFT -> body.translate(ConstVals.PPM / 2f, 0f)
                Direction.RIGHT -> body.translate(-ConstVals.PPM / 2f, 0f)
            }

            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "init()")

            directionOnInit = direction
        }

        override fun act(delta: Float) {
            groundSlideTimer.update(delta)

            val facingBlockLeft = (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT))
            val facingBlockRight = (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
            if (damaged) {
                GameLogger.debug(
                    MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "blocked from act: " +
                        "damaged=$damaged, " +
                        "facingBlockLeft=$facingBlockLeft, " +
                        "facingBlockRight=$facingBlockRight"
                )
                return
            }

            var impulse = (when {
                body.isSensing(BodySense.IN_WATER) -> MegamanValues.WATER_GROUND_SLIDE_VEL
                else -> MegamanValues.GROUND_SLIDE_VEL
            }) * ConstVals.PPM * movementScalar * facing.value
            if (has(MegaEnhancement.GROUND_SLIDE_BOOST)) impulse *= MegaEnhancement.GROUND_SLIDE_BOOST_SCALAR

            when (direction) {
                Direction.UP, Direction.DOWN -> body.physics.velocity.x = impulse
                Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y = impulse
            }
        }

        override fun end() {
            groundSlideTimer.reset()
            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "end()")
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
                when {
                    direction.isVertical() -> {
                        if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) when {
                            direction == Direction.DOWN &&
                                center.y + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getY() ->
                                return false

                            center.y - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxY() ->
                                return false
                        }

                        if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) when {
                            direction == Direction.DOWN &&
                                center.y - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxY() ->
                                return false

                            center.y + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getY() ->
                                return false
                        }
                    }

                    else -> {
                        if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) when {
                            direction == Direction.LEFT &&
                                center.x + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getX() ->
                                return false

                            center.x - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxX() ->
                                return false
                        }

                        if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) when {
                            direction == Direction.LEFT &&
                                center.x + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxX() ->
                                return false

                            center.x - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getX() ->
                                return false
                        }
                    }
                }

                return !game.controllerPoller.isJustPressed(MegaControllerButton.A)
            }

            if (body.isSensing(BodySense.FEET_TOUCHING_LADDER) &&
                game.controllerPoller.isPressed(MegaControllerButton.DOWN)
            ) return true

            if (body.isSensing(BodySense.HEAD_TOUCHING_LADDER) &&
                game.controllerPoller.isPressed(MegaControllerButton.UP)
            ) return true

            return false
        }

        override fun init() {
            aButtonTask = if (body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM else AButtonTask.AIR_DASH
            body.physics.gravityOn = false
            canMakeLandSound = false

            when (direction) {
                Direction.UP, Direction.DOWN -> {
                    body.setCenterX(ladder.body.getCenter().x)

                    if (body.getMaxY() <= ladder.body.getY()) body.setY(ladder.body.getY())
                    else if (body.getY() >= ladder.body.getMaxY()) body.setMaxY(ladder.body.getMaxY())
                }

                Direction.LEFT, Direction.RIGHT -> {
                    body.setCenterY(ladder.body.getCenter().y)

                    if (body.getMaxX() <= ladder.body.getX()) body.setX(ladder.body.getX())
                    else if (body.getX() >= ladder.body.getMaxX()) body.setMaxX(ladder.body.getMaxX())
                }
            }
            body.physics.velocity.setZero()
        }

        override fun act(delta: Float) {
            when (direction) {
                Direction.UP, Direction.DOWN -> body.setCenterX(ladder.body.getCenter().x)

                Direction.LEFT, Direction.RIGHT -> body.setCenterY(ladder.body.getCenter().y)
            }

            if (shooting || game.isProperty(ConstKeys.ROOM_TRANSITION, true)) {
                GameLogger.debug(MEGAMAN_CLIMB_BEHAVIOR_TAG, "Shooting or room transition")
                body.physics.velocity.setZero()
                return
            }

            val velocity = GameObjectPools.fetch(Vector2::class)
            when (direction) {
                Direction.UP -> when {
                    game.controllerPoller.isPressed(MegaControllerButton.UP) -> velocity.set(
                        0f,
                        MegamanValues.CLIMB_VEL
                    )

                    game.controllerPoller.isPressed(MegaControllerButton.DOWN) -> velocity.set(
                        0f,
                        MegamanValues.CLIMB_VEL * -1f
                    )

                    else -> velocity.setZero()
                }

                Direction.DOWN -> when {
                    game.controllerPoller.isPressed(MegaControllerButton.DOWN) -> velocity.set(
                        0f,
                        MegamanValues.CLIMB_VEL
                    )

                    game.controllerPoller.isPressed(MegaControllerButton.UP) -> velocity.set(
                        0f,
                        MegamanValues.CLIMB_VEL * -1f
                    )

                    else -> velocity.setZero()
                }

                Direction.LEFT -> when {
                    game.controllerPoller.isPressed(MegaControllerButton.UP) ->
                        velocity.set(MegamanValues.CLIMB_VEL * -1f, 0f)

                    game.controllerPoller.isPressed(MegaControllerButton.DOWN) ->
                        velocity.set(MegamanValues.CLIMB_VEL, 0f)

                    else -> velocity.setZero()

                }

                Direction.RIGHT -> when {
                    game.controllerPoller.isPressed(MegaControllerButton.UP) -> velocity.set(
                        MegamanValues.CLIMB_VEL,
                        0f
                    )

                    game.controllerPoller.isPressed(MegaControllerButton.DOWN) -> velocity.set(
                        MegamanValues.CLIMB_VEL * -1f,
                        0f
                    )

                    else -> velocity.setZero()
                }
            }.scl(ConstVals.PPM * movementScalar)
            body.physics.velocity.set(velocity)
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
                !game.controllerPoller.areAllPressed(gdxArrayOf(MegaControllerButton.A, MegaControllerButton.UP))

        override fun init() {
            body.physics.velocity.setZero()
            aButtonTask = AButtonTask.JUMP

            cart = body.getProperty(ConstKeys.CART, Cart::class)!!
            cart.sprites.values().forEach { it.hidden = true }

            body.setBottomCenterToPoint(cart.body.getPositionPoint(Position.BOTTOM_CENTER))
            body.preProcess.put(ConstKeys.CART) { cart.body.setCenter(body.getCenter()) }
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

            if (!dead) {
                body.translate(0f, ConstVals.PPM / 1.75f)
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
                !game.controllerPoller.areAllPressed(gdxArrayOf(MegaControllerButton.A, MegaControllerButton.UP)) ||
                isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.AIR_DASHING, BehaviorType.GROUND_SLIDING) ||
                body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.IN_WATER) ||
                weaponHandler.isDepleted(MegamanWeapon.RUSH_JETPACK)
            ) return false

            return if (isBehaviorActive(BehaviorType.JETPACKING)) game.controllerPoller.areAllPressed(
                gdxArrayOf(MegaControllerButton.A, MegaControllerButton.UP)
            ) else game.controllerPoller.allMatch(
                objectMapOf(
                    MegaControllerButton.UP pairTo ButtonStatus.PRESSED,
                    MegaControllerButton.A pairTo ButtonStatus.JUST_PRESSED
                )
            )
        }

        override fun init() {
            requestToPlaySound(SoundAsset.JETPACK_SOUND, true)
            body.physics.gravityOn = false
            timePerBitTimer.reset()
        }

        override fun act(delta: Float) {
            when (direction) {
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
    behaviorsComponent.addBehavior(BehaviorType.CROUCHING, crouch)
    behaviorsComponent.addBehavior(BehaviorType.GROUND_SLIDING, groundSlide)
    behaviorsComponent.addBehavior(BehaviorType.CLIMBING, climb)
    behaviorsComponent.addBehavior(BehaviorType.RIDING_CART, ridingCart)
    behaviorsComponent.addBehavior(BehaviorType.JETPACKING, jetpacking)

    return behaviorsComponent
}