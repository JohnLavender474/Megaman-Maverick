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
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.controller.buttons.ButtonStatus
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.special.Ladder
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

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
        evaluate = evaluate@{
            if (dead || !ready || !canMove || body.isSensing(BodySense.FEET_ON_SAND) ||
                isBehaviorActive(BehaviorType.JETPACKING) || !wallSlideNotAllowedTimer.isFinished()
            ) return@evaluate false

            if ((body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) &&
                    game.controllerPoller.isPressed(MegaControllerButton.LEFT)) ||
                (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) &&
                    game.controllerPoller.isPressed(MegaControllerButton.RIGHT))
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

            val friction = MegamanValues.WALL_SLIDE_FRICTION_TO_APPLY * ConstVals.PPM
            when {
                direction.isVertical() -> body.physics.frictionOnSelf.y = friction
                else -> body.physics.frictionOnSelf.x = friction
            }
        },
        end = {
            if (!body.isSensing(BodySense.IN_WATER)) aButtonTask = AButtonTask.AIR_DASH
            GameLogger.debug(MEGAMAN_WALL_SLIDE_BEHAVIOR_TAG, "end()")
        })

    val swim = object : AbstractBehaviorImpl() {

        private val timer = Timer(MegamanValues.SWIM_TIMER)

        override fun evaluate(delta: Float): Boolean {
            if (dead || !ready || !canMove) return false

            if (damaged || !body.isSensing(BodySense.IN_WATER) || body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))
                return false

            return when {
                isBehaviorActive(BehaviorType.SWIMMING) -> return !timer.isFinished()
                else -> game.controllerPoller.isJustPressed(MegaControllerButton.A) && aButtonTask == AButtonTask.SWIM
            }
        }

        override fun init() {
            GameLogger.debug(MEGAMAN_SWIM_BEHAVIOR_TAG, "Init method called")

            timer.reset()

            val impulse = GameObjectPools.fetch(Vector2::class)

            when (direction) {
                Direction.UP -> impulse.set(0f, swimVel)
                Direction.DOWN -> impulse.set(0f, -swimVel)
                Direction.LEFT -> impulse.set(swimVel, 0f)
                Direction.RIGHT -> impulse.set(-swimVel, 0f)
            }.scl(ConstVals.PPM.toFloat())

            if (hasEnhancement(MegaEnhancement.JUMP_BOOST)) impulse.scl(MegaEnhancement.JUMP_BOOST_SCALAR)

            body.physics.velocity.add(impulse)
        }

        override fun act(delta: Float) = timer.update(delta)

        override fun end() = timer.reset()
    }

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
                    hasEnhancement(MegaEnhancement.JUMP_BOOST) -> jumpVel * MegaEnhancement.JUMP_BOOST_SCALAR
                    else -> jumpVel
                }
            }
            v.y = when (direction) {
                Direction.UP, Direction.DOWN -> ConstVals.PPM * when {
                    isBehaviorActive(BehaviorType.WALL_SLIDING) -> wallJumpVel
                    hasEnhancement(MegaEnhancement.JUMP_BOOST) -> jumpVel * MegaEnhancement.JUMP_BOOST_SCALAR
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

        private val minTimer = Timer(MegamanValues.AIR_DASH_MIN_TIME)
        private val maxTimer = Timer(MegamanValues.AIR_DASH_MAX_TIME)
        private val impulse = Vector2()
        private var lastFacing = Facing.RIGHT

        private fun isAirDashButtonActivated() = when (game.selectButtonAction) {
            SelectButtonAction.AIR_DASH -> game.controllerPoller.isPressed(MegaControllerButton.SELECT)
            else -> game.controllerPoller.isPressed(MegaControllerButton.A)
        }

        private fun isAirDashButtonJustActivated() = when (game.selectButtonAction) {
            SelectButtonAction.AIR_DASH -> game.controllerPoller.isJustPressed(MegaControllerButton.SELECT)
            else -> game.controllerPoller.isJustPressed(MegaControllerButton.A)
        }

        override fun evaluate(delta: Float): Boolean {
            if (dead || !ready || !canMove || damaged || teleporting || maxTimer.isFinished() ||
                body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.TELEPORTING) || isAnyBehaviorActive(
                    BehaviorType.WALL_SLIDING, BehaviorType.CLIMBING, BehaviorType.JETPACKING
                )
            ) return false

            if (isBehaviorActive(BehaviorType.AIR_DASHING)) return !minTimer.isFinished() || isAirDashButtonActivated()

            return aButtonTask == AButtonTask.AIR_DASH && isAirDashButtonJustActivated() &&
                (currentWeapon != MegamanWeapon.RUSH_JET || game.controllerPoller.isReleased(MegaControllerButton.UP))
        }

        override fun init() {
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "init()")

            minTimer.reset()
            maxTimer.reset()

            body.physics.gravityOn = false
            aButtonTask = AButtonTask.JUMP

            if (direction.isVertical()) impulse.y = 0f else impulse.x = 0f

            var impulseValue = ConstVals.PPM * movementScalar *
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_VEL else MegamanValues.AIR_DASH_VEL)
            if (hasEnhancement(MegaEnhancement.AIR_DASH_BOOST)) impulseValue *= MegaEnhancement.AIR_DASH_BOOST_SCALAR

            when (direction) {
                Direction.UP -> impulse.x = impulseValue * facing.value
                Direction.DOWN -> impulse.x = impulseValue * -facing.value
                Direction.LEFT -> impulse.y = impulseValue * facing.value
                Direction.RIGHT -> impulse.y = impulseValue * -facing.value
            }

            lastFacing = facing

            putProperty(MegamanKeys.DIRECTION_ON_AIR_DASH, direction)
        }

        override fun act(delta: Float) {
            minTimer.update(delta)
            maxTimer.update(delta)

            if (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)
            ) return

            if (facing != lastFacing) impulse.scl(-1f)
            lastFacing = facing

            body.physics.velocity.set(impulse)
        }

        override fun end() {
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "end()")

            minTimer.reset()
            maxTimer.reset()

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
            // always update the timer if Megaman has the crouch ability, even when the `if` below returns false
            when {
                !game.controllerPoller.isPressed(MegaControllerButton.DOWN) ||
                    isBehaviorActive(BehaviorType.GROUND_SLIDING) -> timer.reset()
                else -> timer.update(delta)
            }

            if (dead || damaged || !ready || !canMove || game.isCameraRotating() ||
                isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.JETPACKING) ||
                !body.isSensing(BodySense.FEET_ON_GROUND) || body.isSensing(BodySense.FEET_ON_SAND) ||
                !game.controllerPoller.isPressed(MegaControllerButton.DOWN) ||
                (currentWeapon == MegamanWeapon.AXE_SWINGER && shooting) ||
                when {
                    direction.isVertical() -> abs(body.physics.velocity.x)
                    else -> abs(body.physics.velocity.y)
                } > MegamanValues.CROUCH_MAX_VEL * ConstVals.PPM
            ) return false

            return timer.isFinished()
        }

        override fun init() {
            body.physics.velocity.setZero()

            when (direction) {
                Direction.UP -> {}
                Direction.DOWN -> body.translate(0f, 0.75f * ConstVals.PPM)
                Direction.LEFT -> body.translate(0.75f * ConstVals.PPM, 0f)
                Direction.RIGHT -> body.translate(-0.75f * ConstVals.PPM, 0f)
            }
        }

        override fun act(delta: Float) {
            body.physics.velocity.x = 0f
        }
    }

    val groundSlide = object : AbstractBehaviorImpl() {

        private val minTimer = Timer(MegamanValues.GROUND_SLIDE_MIN_TIME)
        private val maxTimer = Timer(MegamanValues.GROUND_SLIDE_MAX_TIME)
        private val cooldown = Timer(MegamanValues.GROUND_SLIDE_COOLDOWN)
        private var directionOnInit: Direction? = null

        override fun evaluate(delta: Float): Boolean {
            cooldown.update(delta)

            if (dead || !ready || !canMove || game.isCameraRotating() || body.isSensing(BodySense.FEET_ON_SAND) ||
                isBehaviorActive(BehaviorType.JETPACKING) || !body.isSensing(BodySense.FEET_ON_GROUND) ||
                !cooldown.isFinished()
            ) return false

            if (isBehaviorActive(BehaviorType.GROUND_SLIDING) && body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))
                return true

            if (damaged || maxTimer.isFinished() ||
                (minTimer.isFinished() && !game.controllerPoller.isPressed(MegaControllerButton.DOWN))
            ) return false

            return when {
                isBehaviorActive(BehaviorType.GROUND_SLIDING) -> !minTimer.isFinished() ||
                    (game.controllerPoller.isPressed(MegaControllerButton.A) && directionOnInit == direction)
                else -> game.controllerPoller.isPressed(MegaControllerButton.DOWN) &&
                    game.controllerPoller.isJustPressed(MegaControllerButton.A)
            }
        }

        override fun init() {
            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "init()")

            minTimer.reset()
            maxTimer.reset()

            when (direction) {
                Direction.UP -> {}
                Direction.DOWN -> body.translate(0f, 0.75f * ConstVals.PPM)
                Direction.LEFT -> body.translate(0.75f * ConstVals.PPM, 0f)
                Direction.RIGHT -> body.translate(-0.75f * ConstVals.PPM, 0f)
            }

            directionOnInit = direction

            runTime = 100f
        }

        override fun act(delta: Float) {
            minTimer.update(delta)
            maxTimer.update(delta)

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

            var vel = (when {
                body.isSensing(BodySense.IN_WATER) -> MegamanValues.WATER_GROUND_SLIDE_VEL
                else -> MegamanValues.GROUND_SLIDE_VEL
            }) * ConstVals.PPM * movementScalar

            if (hasEnhancement(MegaEnhancement.GROUND_SLIDE_BOOST)) vel *= MegaEnhancement.GROUND_SLIDE_BOOST_SCALAR

            when (direction) {
                Direction.UP -> body.physics.velocity.x = vel * facing.value
                Direction.DOWN -> body.physics.velocity.x = vel * -facing.value
                Direction.LEFT -> body.physics.velocity.y = vel * facing.value
                Direction.RIGHT -> body.physics.velocity.y = vel * -facing.value
            }
        }

        override fun end() {
            minTimer.reset()
            maxTimer.reset()
            cooldown.reset()
            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "end()")
        }
    }

    val climb = object : AbstractBehaviorImpl() {

        private lateinit var ladder: Ladder

        override fun evaluate(delta: Float): Boolean {
            if (dead || !ready || !canMove) return false

            if (damaged || !body.properties.containsKey(ConstKeys.LADDER) || isAnyBehaviorActive(
                    BehaviorType.JUMPING,
                    BehaviorType.SWIMMING,
                    BehaviorType.JETPACKING,
                    BehaviorType.AIR_DASHING,
                    BehaviorType.GROUND_SLIDING
                )
            ) return false

            ladder = body.getProperty(ConstKeys.LADDER, Ladder::class)!!

            val headPos = body.fixtures.get(FixtureType.HEAD)
                .first().getShape().getBoundingRectangle()
                .getPositionPoint(if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT)
            val headBounds = GameObjectPools.fetch(GameRectangle::class)
                .setSize(0.25f * ConstVals.PPM)
                .setCenter(headPos)

            val feetPos = body.fixtures.get(FixtureType.FEET)
                .first().getShape().getBoundingRectangle()
                .getPositionPoint(if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT)
            val feetBounds = GameObjectPools.fetch(GameRectangle::class)
                .setSize(0.25f * ConstVals.PPM)
                .setCenter(feetPos)

            val bodyCenter = body.getCenter()

            if (isBehaviorActive(BehaviorType.CLIMBING)) {
                when {
                    direction.isVertical() -> {
                        if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) when {
                            direction == Direction.DOWN &&
                                bodyCenter.y + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getY() ->
                                return false

                            bodyCenter.y - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxY() ->
                                return false
                        }

                        if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) when {
                            direction == Direction.DOWN &&
                                bodyCenter.y - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxY() ->
                                return false

                            bodyCenter.y + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getY() ->
                                return false
                        }
                    }
                    else -> {
                        if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) when {
                            direction == Direction.LEFT &&
                                bodyCenter.x + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getX() ->
                                return false

                            bodyCenter.x - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxX() ->
                                return false
                        }

                        if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) when {
                            direction == Direction.LEFT &&
                                bodyCenter.x + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxX() ->
                                return false

                            bodyCenter.x - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getX() ->
                                return false
                        }
                    }
                }

                return !game.controllerPoller.isJustPressed(MegaControllerButton.A)
            }

            if (body.isSensing(BodySense.FEET_TOUCHING_LADDER) &&
                ladder.body.getBounds().overlaps(feetBounds) &&
                game.controllerPoller.isPressed(MegaControllerButton.DOWN)
            ) return true

            if (body.isSensing(BodySense.HEAD_TOUCHING_LADDER) &&
                ladder.body.getBounds().overlaps(headBounds) &&
                game.controllerPoller.isPressed(MegaControllerButton.UP)
            ) return true

            return false
        }

        override fun init() {
            game.setFocusSnappedAway(true)

            aButtonTask = if (body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM else AButtonTask.AIR_DASH
            body.physics.gravityOn = false
            canMakeLandSound = false

            when (direction) {
                Direction.UP, Direction.DOWN -> {
                    body.setCenterX(ladder.body.getCenter().x)

                    when {
                        body.getMaxY() <= ladder.body.getY() -> body.setY(ladder.body.getY())
                        body.getY() >= ladder.body.getMaxY() -> body.setMaxY(ladder.body.getMaxY())
                    }
                }
                Direction.LEFT, Direction.RIGHT -> {
                    body.setCenterY(ladder.body.getCenter().y)

                    when {
                        body.getMaxX() <= ladder.body.getX() -> body.setX(ladder.body.getX())
                        body.getX() >= ladder.body.getMaxX() -> body.setMaxX(ladder.body.getMaxX())
                    }
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
                GameLogger.debug(MEGAMAN_CLIMB_BEHAVIOR_TAG, "shooting or room transition")
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
            game.setFocusSnappedAway(false)
            body.physics.gravityOn = true
            body.physics.velocity.setZero()
            aButtonTask = if (body.isSensing(BodySense.IN_WATER)) AButtonTask.SWIM else AButtonTask.AIR_DASH
        }
    }

    val jetpacking = object : AbstractBehaviorImpl() {

        private val timePerBitTimer = Timer(MegamanValues.JETPACK_TIME_PER_BIT)

        override fun evaluate(delta: Float): Boolean {
            if (dead || !ready || !canMove || damaged || teleporting || currentWeapon != MegamanWeapon.RUSH_JET ||
                !game.controllerPoller.areAllPressed(gdxArrayOf(MegaControllerButton.A, MegaControllerButton.UP)) ||
                isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.AIR_DASHING, BehaviorType.GROUND_SLIDING) ||
                weaponsHandler.isDepleted(MegamanWeapon.RUSH_JET)
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
            body.physics.gravityOn = false

            val impulse = MegamanValues.JETPACK_Y_IMPULSE * ConstVals.PPM * movementScalar * delta

            when (direction) {
                Direction.UP -> {
                    if (body.physics.velocity.y < 0f) body.physics.velocity.y = 0f
                    body.physics.velocity.y += impulse
                }
                Direction.DOWN -> {
                    if (body.physics.velocity.y > 0f) body.physics.velocity.y = 0f
                    body.physics.velocity.y += -impulse
                }
                Direction.LEFT -> {
                    if (body.physics.velocity.x > 0f) body.physics.velocity.x = 0f
                    body.physics.velocity.x += -impulse
                }
                Direction.RIGHT -> {
                    if (body.physics.velocity.x < 0f) body.physics.velocity.x = 0f
                    body.physics.velocity.x += impulse
                }
            }

            timePerBitTimer.update(delta)

            if (timePerBitTimer.isFinished()) {
                weaponsHandler.translateAmmo(MegamanWeapon.RUSH_JET, -1)
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
    behaviorsComponent.addBehavior(BehaviorType.JETPACKING, jetpacking)

    return behaviorsComponent
}
