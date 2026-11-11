package com.megaman.maverick.game.entities.megamanx

import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.players.AirDashParams
import com.megaman.maverick.game.players.JumpParams
import com.megaman.maverick.game.players.WallSlideParams
import com.megaman.maverick.game.players.bodyOverGround
import com.megaman.maverick.game.players.isRunSidePressed
import com.megaman.maverick.game.players.playerAirDashBehavior
import com.megaman.maverick.game.players.playerJumpBehavior
import com.megaman.maverick.game.players.playerWallSlideBehavior
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.entities.state.stunned
import com.megaman.maverick.game.players.playerCanMove
import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.teleporting
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.badlogic.gdx.math.MathUtils
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.getBody
import com.megaman.maverick.game.world.body.hasBodyLabel
import com.megaman.maverick.game.world.body.receiver
import kotlin.math.max
import kotlin.math.min
import com.maverick.game.engine.components.require
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.running

// TODO: SELECT is always X's dash button for now, whatever the slot's select action is. The dash button changed
//  several times (a new R button in 081c; DOWN + A added and then removed in 081d; R replaced by SELECT in 081d), and
//  no adequate solution exists yet; the dash input of X stays open in item 081
internal fun MegamanX.dashJustPressed() = input.isJustPressed(MegaControllerButton.SELECT)

internal fun MegamanX.dashHeld() = input.isPressed(MegaControllerButton.SELECT)

internal fun MegamanX.dashPressNow() = dashJustPressed() || dashPressKept

internal fun MegamanX.resetAirMovesOnLanding() {
    val landed = bodyOverGround && !isBehaviorActive(BehaviorType.JUMPING)
    if (landed) {
        airDashUsed = false
        dashSpeedInAir = false
        if (airborne) require(RunComponent::class).runTime = 0f
    }
    airborne = !landed
}

// TODO: a press is kept for 1 render frame, not 1 world step; on a frame with no world step the landing comes 1 frame
//  later and the kept press can run out first (the engine timing, item 134)
internal fun MegamanX.keepStartPresses() {
    val keep = (airborne && bodyOverGround) || pressHeldForLanding
    pressHeldForLanding = false
    jumpPressKept = keep && input.isJustPressed(MegaControllerButton.A) &&
        !isBehaviorActive(BehaviorType.JUMPING) && !isBehaviorActive(BehaviorType.AIR_DASHING)
    dashPressKept = keep && dashJustPressed() && !isBehaviorActive(BehaviorType.GROUND_DASHING) &&
        !isBehaviorActive(BehaviorType.AIR_DASHING)
    if (!kickLocked && !isBehaviorActive(BehaviorType.JUMPING)) wallKicked = false
}

private const val LANDING_PROBE_FRAMES = 1.5f

// TODO: the probe uses 1.5 nominal frames of the performance FPS, not the real frame time; on a frame that is longer
//  than that, the landing is not seen and an A or SELECT press on the landing frame gives a low air dash, as before
//  (the engine timing, item 134); the probe also does not move a moving platform ahead, so a landing on a rising
//  platform can be missed in the same way
internal fun MegamanX.landsWithinFrame(): Boolean {
    val container = services.levelState.worldContainer
        ?: throw IllegalStateException("MegamanX.landsWithinFrame: the level has no world container")
    val fallSpeed = -upSpeed()
    val frameTime = LANDING_PROBE_FRAMES / services.settings.performance.fps
    val gravity = if (fallSpeed < 0f) MegamanXValues.JUMP_GRAVITY else MegamanXValues.FALL_GRAVITY
    val nextFallSpeed = min(fallSpeed - gravity * ConstVals.PPM * frameTime,
        MegamanXValues.FALL_MAX * ConstVals.PPM)
    if (nextFallSpeed <= 0f) return false
    val distance = nextFallSpeed * frameTime

    val feet = body.getProperty("${ConstKeys.FEET}_${ConstKeys.GRAVITY}") as IFixture
    val probe = feet.getShape().getBoundingRectangle(landingProbe)
    when (direction) {
        Direction.UP -> probe.set(probe.getX(), probe.getY() - distance, probe.getWidth(), probe.getHeight() + distance)
        Direction.DOWN -> probe.setHeight(probe.getHeight() + distance)
        Direction.LEFT -> probe.setWidth(probe.getWidth() + distance)
        Direction.RIGHT -> probe.set(probe.getX() - distance, probe.getY(), probe.getWidth() + distance, probe.getHeight())
    }

    var found = false
    container.forEachFixture(
        MathUtils.floor(probe.getX() / ConstVals.PPM),
        MathUtils.floor(probe.getY() / ConstVals.PPM),
        MathUtils.ceil(probe.getMaxX() / ConstVals.PPM),
        MathUtils.ceil(probe.getMaxY() / ConstVals.PPM)
    ) { fixture, _ ->
        if (fixture.isActive() && feet.receiver(Receivers.FILTER)?.invoke(fixture) != false &&
            !(fixture.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY) && input.isPressed(MegaControllerButton.DOWN)) &&
            fixture.getShape().overlaps(probe)
        ) found = true
        !found
    }
    return found
}

internal fun MegamanX.runPressedSide() = when {
    isRunSidePressed(Facing.LEFT) -> run(0f, Facing.LEFT)
    isRunSidePressed(Facing.RIGHT) -> run(0f, Facing.RIGHT)
    else -> Unit
}

internal fun MegamanX.endKickLockOnBlock() {
    if (!kickLocked) return
    val awaySense = when (facing.opposite()) {
        Facing.LEFT -> BodySense.SIDE_TOUCHING_BLOCK_LEFT
        Facing.RIGHT -> BodySense.SIDE_TOUCHING_BLOCK_RIGHT
    }
    if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) || body.isSensing(awaySense)) {
        kickLockTimer.setToEnd()
        setSideSpeed(0f)
    }
}

internal val MegamanX.kickLocked: Boolean
    get() = !kickLockTimer.isFinished()

internal val MegamanX.pushingOff: Boolean
    get() = !pushOffTimer.isFinished()

internal val MegamanX.holdsSideSpeed: Boolean
    get() = kickLocked || pushingOff ||
        isBehaviorActive(BehaviorType.AIR_DASHING) || isBehaviorActive(BehaviorType.GROUND_DASHING)

internal val MegamanX.holdsSideSpeedForRun: Boolean
    get() = kickLocked || pushingOff || isBehaviorActive(BehaviorType.AIR_DASHING) ||
        (isBehaviorActive(BehaviorType.GROUND_DASHING) && bodyOverGround && !isBehaviorActive(BehaviorType.JUMPING))

internal fun MegamanX.touchedWallSide(): Facing? = when {
    body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) -> Facing.LEFT
    body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) -> Facing.RIGHT
    else -> null
}

internal fun MegamanX.upSpeed() = when (direction) {
    Direction.UP -> body.physics.velocity.y
    Direction.DOWN -> -body.physics.velocity.y
    Direction.LEFT -> -body.physics.velocity.x
    Direction.RIGHT -> body.physics.velocity.x
}

internal fun MegamanX.setUpSpeed(speed: Float) = when (direction) {
    Direction.UP -> body.physics.velocity.y = speed
    Direction.DOWN -> body.physics.velocity.y = -speed
    Direction.LEFT -> body.physics.velocity.x = -speed
    Direction.RIGHT -> body.physics.velocity.x = speed
}

internal fun MegamanX.setSideSpeed(speed: Float) = when (direction) {
    Direction.UP -> body.physics.velocity.x = speed
    Direction.DOWN -> body.physics.velocity.x = -speed
    Direction.LEFT -> body.physics.velocity.y = speed
    Direction.RIGHT -> body.physics.velocity.y = -speed
}

internal fun MegamanX.wallSlideBehavior() = playerWallSlideBehavior(
    this,
    WallSlideParams(
        blocked = { kickLocked },
        allowedWhileJumping = { wallDelayTimer.isFinished() },
        holdsWithoutInput = {
            (!wallGrabTimer.isFinished() || sabre.kind == SabreKind.WALL) && touchedWallSide() != null
        },
        onStart = {
            wallGrabTimer.reset()
            airDashUsed = false
            dashSpeedInAir = false
        },
        onAct = { delta ->
            wallGrabTimer.update(delta)
            body.physics.gravityOn = false
            body.physics.velocity.setZero()
            if (wallGrabTimer.isFinished() && sabre.kind != SabreKind.WALL)
                setUpSpeed(-MegamanXValues.WALL_SLIDE_SPEED * ConstVals.PPM)
        },
        onEnd = {
            val wall = touchedWallSide()
            if (wall != null && wallGrabTimer.isFinished() && !isRunSidePressed(wall) &&
                !isBehaviorActive(BehaviorType.JUMPING) && !bodyOverGround && playerCanMove && !damaged && !stunned
            ) {
                pushOffTimer.reset()
                setSideSpeed(MegamanXValues.PUSH_OFF_SPEED * ConstVals.PPM * facing.value)
            } else runPressedSide()
        }
    )
)

internal fun MegamanX.jumpBehavior() = playerJumpBehavior(
    this,
    JumpParams(
        canStart = { true },
        blocked = {
            isBehaviorActive(BehaviorType.AIR_DASHING) ||
                (sabre.kind == SabreKind.FLOOR && sabre.time < MegamanXValues.SABRE_FLOOR_JUMP_CANCEL_TIME)
        },
        jumpSpeed = { upSign() * MegamanXValues.JUMP_SPEED },
        wallJumpSpeed = { upSign() * MegamanXValues.WALL_KICK_SPEED },
        wallJumpHorizontal = 0f,
        wallJumpAllowed = {
            isBehaviorActive(BehaviorType.WALL_SLIDING) ||
                (!bodyOverGround && touchedWallSide() != null && wallDelayTimer.isFinished())
        },
        ignoreRelease = { kickLocked || sabre.kind == SabreKind.AIR },
        startPressed = { input.isJustPressed(MegaControllerButton.A) || jumpPressKept },
        downBlocksGroundJump = false,
        endKeepsFall = true,
        onJump = {
            sabre.end()
            wallDelayTimer.reset()
            wallKicked = false
            val dashHeld = dashHeld()
            val wall = touchedWallSide()
            if (!bodyOverGround && wall != null) {
                facing = wall
                val away = if (dashHeld) MegamanXValues.DASH_WALL_KICK_AWAY_SPEED else MegamanXValues.WALL_KICK_AWAY_SPEED
                setSideSpeed(away * ConstVals.PPM * wall.opposite().value)
                kickLockTimer.reset()
                wallKicked = true
                pushOffTimer.setToEnd()
                dashSpeedInAir = dashHeld
                airDashUsed = dashHeld
            } else if (dashHeld || groundDashJumpable) {
                dashSpeedInAir = true
                airDashUsed = true
            }
        }
    )
)

internal fun MegamanX.airDashBehavior() = playerAirDashBehavior(
    this,
    AirDashParams(
        blocked = {
            bodyOverGround || (isBehaviorActive(BehaviorType.AIR_DASHING) && isRunSidePressed(facing.opposite()))
        },
        canStart = {
            val aPressed = input.isJustPressed(MegaControllerButton.A) || jumpPressKept
            val jumpTookA = isBehaviorActive(BehaviorType.JUMPING) && aPressed
            val pressed = dashPressNow() || (aPressed && !jumpTookA)
            val fromKeptPress = jumpPressKept || dashPressKept
            when {
                airDashUsed || kickLocked || pushingOff || sabre.active || !pressed -> false
                !fromKeptPress && landsWithinFrame() -> {
                    pressHeldForLanding = true
                    false
                }
                else -> true
            }
        },
        held = { false },
        startDelay = MegamanXValues.AIR_DASH_HOVER_TIME,
        minTime = MegamanXValues.AIR_DASH_HOVER_TIME + MegamanXValues.AIR_DASH_TIME,
        maxTime = { MegamanXValues.AIR_DASH_HOVER_TIME + MegamanXValues.AIR_DASH_TIME },
        speed = {
            if (dashPressNow()) MegamanXValues.AIR_DASH_FAST_START_SPEED
            else MegamanXValues.AIR_DASH_SPEED
        },
        decel = { if (dashPressNow()) MegamanXValues.DASH_DECEL else 0f },
        minSpeed = MegamanXValues.AIR_DASH_SPEED,
        endBump = { 0f },
        onStart = {
            airDashUsed = true
            when {
                isRunSidePressed(Facing.LEFT) -> facing = Facing.LEFT
                isRunSidePressed(Facing.RIGHT) -> facing = Facing.RIGHT
            }
        },
        onEnd = { runPressedSide() }
    )
)

internal class GroundDash(private val x: MegamanX) : AbstractBehaviorImpl() {

    var time = 0f
        private set
    private var speed = 0f
    private var ending = false
    private var endTime = 0f

    val jumpable: Boolean
        get() = isActive() && !ending

    val braking: Boolean
        get() = isActive() && ending

    private fun startPressed() = x.dashJustPressed() || x.dashPressKept

    private fun inputHeld() = x.dashHeld() ||
        x.isRunSidePressed(Facing.LEFT) || x.isRunSidePressed(Facing.RIGHT)

    override fun evaluate(delta: Float): Boolean {
        if (!x.existenceState.liveAfterUpdate || !x.inPlay || !x.playerCanMove || x.damaged || x.teleporting ||
            !x.bodyOverGround || x.isBehaviorActive(BehaviorType.JUMPING) || x.kickLocked || x.sabre.active ||
            FacingUtils.isFacingBlock(x)
        ) return false

        if (isActive()) {
            if (ending && startPressed()) {
                init()
                return true
            }
            if (ending) return endTime < MegamanXValues.DASH_END_TIME
            return true
        }

        return startPressed()
    }

    override fun init(vararg params: Any) {
        time = 0f
        endTime = 0f
        ending = false
        speed = MegamanXValues.DASH_START_SPEED
        when {
            x.isRunSidePressed(Facing.LEFT) -> x.facing = Facing.LEFT
            x.isRunSidePressed(Facing.RIGHT) -> x.facing = Facing.RIGHT
        }
    }

    override fun act(delta: Float) {
        time += delta
        if (time < MegamanXValues.DASH_START_DELAY) {
            x.setSideSpeed(0f)
            return
        }

        if (!ending && (time >= MegamanXValues.DASH_START_DELAY + MegamanXValues.DASH_TIME || !inputHeld() ||
                x.isRunSidePressed(x.facing.opposite()))
        ) {
            ending = true
            speed = min(speed, MegamanXValues.DASH_SPEED)
        }
        if (ending) endTime += delta

        val floor = if (ending) 0f else MegamanXValues.DASH_SPEED
        speed = max(floor, speed - MegamanXValues.DASH_DECEL * delta)
        x.setSideSpeed(speed * ConstVals.PPM * x.facing.value)
    }

    override fun end() {
        ending = false
        if (!x.bodyOverGround) x.airDashUsed = true
        if (x.bodyOverGround && x.isRunSidePressed(x.facing) && !x.damaged && !x.sabre.active &&
            !x.isBehaviorActive(BehaviorType.JUMPING)
        ) {
            x.require(RunComponent::class).runTime = 0f
            x.running = true
            x.setSideSpeed(MegamanXValues.WALK_START_SPEED * ConstVals.PPM * x.facing.value)
        }
    }
}

internal fun MegamanX.groundDashBehavior(): IBehavior = GroundDash(this).also { groundDash = it }

internal val MegamanX.groundDashJumpable: Boolean
    get() = groundDash?.jumpable == true

internal val MegamanX.groundDashBraking: Boolean
    get() = groundDash?.braking == true

