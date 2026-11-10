package com.megaman.maverick.game.entities.megamanx

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.Points
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.entities.enemies.MetaWheelF
import com.megaman.maverick.game.entities.state.TagsComponent
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private const val DELTA = 0.02f
private const val TOLERANCE = 0.001f

class MegamanXMovesTest : DescribeSpec({

    lateinit var x: MegamanX
    lateinit var services: MegaServices
    lateinit var body: Body
    lateinit var behaviors: BehaviorsComponent
    lateinit var controllerSystem: ControllerSystem
    var frame = InputFrame.RELEASED
    var overGround = false

    val order = listOf(
        BehaviorType.WALL_SLIDING, BehaviorType.GROUND_DASHING, BehaviorType.JUMPING, BehaviorType.AIR_DASHING
    )

    fun hold(vararg pairs: Pair<MegaControllerButton, ButtonStatus>) {
        frame = InputFrame.RELEASED
        pairs.forEach { (button, status) -> frame = frame.with(button, status) }
    }

    fun tick(delta: Float = DELTA, world: () -> Unit = {}) {
        controllerSystem.update(delta)
        order.forEach { behaviors.getBehavior(it)!!.update(delta) }
        world()
        x.kickLockTimer.update(delta)
        x.pushOffTimer.update(delta)
        x.wallDelayTimer.update(delta)
        x.endKickLockOnBlock()
        x.keepStartPresses()
        x.resetAirMovesOnLanding()
    }

    fun ticks(seconds: Float) {
        var time = 0f
        while (time < seconds - TOLERANCE) {
            tick()
            time += DELTA
        }
    }

    fun active(type: BehaviorType) = behaviors.getBehavior(type)!!.isActive()

    fun touchWall(side: Facing) = body.setBodySense(
        if (side == Facing.LEFT) BodySense.SIDE_TOUCHING_BLOCK_LEFT else BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true
    )

    fun leaveWalls() {
        body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, false)
        body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, false)
    }

    fun startSlideOnLeftWall() {
        touchWall(Facing.LEFT)
        hold(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
        tick()
        hold(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
        tick()
    }

    beforeEach {
        frame = InputFrame.RELEASED
        overGround = false

        services = testServices(engine = mockk<GameEngine>(relaxed = true))
        services.levelState.currentLevel = LevelDefinition.X6_INTRO
        services.levelState.worldContainer = SimpleGridWorldContainer(ConstVals.PPM)

        x = MegamanX(services)
        x.existenceState = EntityExistenceState.LIVE
        x.facing = Facing.RIGHT

        x.addComponent(x.defineBodyComponent())
        body = x.getComponent(BodyComponent::class)!!.body
        body.putProperty(ConstKeys.BODY_OVER_GROUND, { overGround })
        x.addComponent(RunComponent())
        x.addComponent(LocksComponent(MegaLockGroup.entries.size))
        x.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        x.addComponent(TeleportableComponent())
        x.addComponent(x.damageRecovery)
        x.addComponent(x.defineStunComponent())
        x.addComponent(x.defineFreezeComponent())
        x.addComponent(x.defineWeaponChargeComponent())
        x.addComponent(PostActionMomentumComponent(0f, 0f, { 0f }, { overGround }, { false }, { false }))
        x.addComponent(AudioComponent())
        x.addComponent(PointsComponent(ConstKeys.HEALTH pairTo Points(0, ConstVals.MAX_HEALTH, ConstVals.MAX_HEALTH)))
        behaviors = x.defineBehaviorsComponent()
        x.addComponent(behaviors)

        services.players.register(PlayerSlot(0), x)
        x.addComponent(x.defineControllerComponent().also { it.input = InputSource { frame } })

        controllerSystem = ControllerSystem()
        controllerSystem.add(x)
    }

    afterEach { unmockkAll() }

    describe("the wall slide") {
        it("grabs the wall and holds still for the grab time") {
            body.physics.velocity.set(0f, -5f)
            startSlideOnLeftWall()

            active(BehaviorType.WALL_SLIDING) shouldBe true
            body.physics.velocity.y shouldBe 0f
            body.physics.gravityOn shouldBe false
            x.facing shouldBe Facing.RIGHT
        }

        it("slides at the fixed slide speed after the grab") {
            startSlideOnLeftWall()
            ticks(MegamanXValues.WALL_GRAB_TIME + DELTA)

            body.physics.velocity.y shouldBe (-MegamanXValues.WALL_SLIDE_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("gives a new air dash") {
            x.airDashUsed = true
            startSlideOnLeftWall()
            x.airDashUsed shouldBe false
        }

        it("pushes off the wall when the wall side is released after the grab") {
            startSlideOnLeftWall()
            ticks(MegamanXValues.WALL_GRAB_TIME + DELTA)
            hold()
            tick()

            active(BehaviorType.WALL_SLIDING) shouldBe false
            x.pushingOff shouldBe true
            body.physics.velocity.x shouldBe (MegamanXValues.PUSH_OFF_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
            body.physics.gravityOn shouldBe true

            body.preProcess()
            body.physics.velocity.x shouldBe (MegamanXValues.PUSH_OFF_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)

            ticks(MegamanXValues.PUSH_OFF_TIME + DELTA)
            body.preProcess()
            body.physics.velocity.x shouldBe 0f
        }

        it("does not push off when the slide ends from damage") {
            startSlideOnLeftWall()
            x.takeDamageFrom(MetaWheelF(services).also { it.addComponent(TagsComponent(MetaWheelF.TAG)) }) shouldBe true
            tick()

            active(BehaviorType.WALL_SLIDING) shouldBe false
            x.pushingOff shouldBe false
        }

        it("holds the grab when the wall side is released during the grab time") {
            startSlideOnLeftWall()
            hold()
            tick()
            active(BehaviorType.WALL_SLIDING) shouldBe true
            x.pushingOff shouldBe false
        }

        it("grabs a wall while rising, after the wall delay from the jump start") {
            overGround = true
            touchWall(Facing.LEFT)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            overGround = false
            hold(MegaControllerButton.A to ButtonStatus.PRESSED, MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.WALL_SLIDING) shouldBe false
            hold(MegaControllerButton.A to ButtonStatus.PRESSED, MegaControllerButton.LEFT to ButtonStatus.PRESSED)
            ticks(MegamanXValues.WALL_DELAY_AFTER_JUMP)
            active(BehaviorType.WALL_SLIDING) shouldBe true
            tick()
            active(BehaviorType.JUMPING) shouldBe false
        }

        it("does not grab on the ground") {
            body.setBodySense(BodySense.FEET_ON_GROUND, true)
            startSlideOnLeftWall()
            active(BehaviorType.WALL_SLIDING) shouldBe false
        }

        it("ignores the run input while it slides") {
            startSlideOnLeftWall()
            x.running shouldBe false
            body.physics.velocity.x shouldBe 0f
        }
    }

    describe("the wall kick") {
        it("kicks up and away from the wall from a slide, and locks the input") {
            startSlideOnLeftWall()
            hold(MegaControllerButton.LEFT to ButtonStatus.PRESSED, MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()

            active(BehaviorType.JUMPING) shouldBe true
            body.physics.velocity.y shouldBe (MegamanXValues.WALL_KICK_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
            body.physics.velocity.x shouldBe
                (MegamanXValues.WALL_KICK_AWAY_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
            x.facing shouldBe Facing.LEFT
            x.kickLocked shouldBe true
        }

        it("kicks on a wall touch with no slide") {
            touchWall(Facing.RIGHT)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()

            body.physics.velocity.y shouldBe (MegamanXValues.WALL_KICK_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
            body.physics.velocity.x shouldBe
                (-MegamanXValues.WALL_KICK_AWAY_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("keeps the kick speeds through the lock, with A released and a side held") {
            touchWall(Facing.RIGHT)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            leaveWalls()
            hold(MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED)
            tick()
            body.preProcess()

            active(BehaviorType.JUMPING) shouldBe true
            body.physics.velocity.x shouldBe
                (-MegamanXValues.WALL_KICK_AWAY_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("ends the lock and the away speed on a ceiling") {
            touchWall(Facing.RIGHT)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            x.kickLocked shouldBe true
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
            hold(MegaControllerButton.A to ButtonStatus.PRESSED)
            tick()
            x.kickLocked shouldBe false
            body.physics.velocity.x shouldBe 0f
        }

        it("ends the lock after the lock time") {
            touchWall(Facing.RIGHT)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.A to ButtonStatus.PRESSED)
            ticks(MegamanXValues.WALL_KICK_LOCK_TIME + DELTA)
            x.kickLocked shouldBe false
        }

        it("does not kick on a wall touch within the wall delay after a jump start") {
            overGround = true
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            overGround = false
            hold()
            tick()
            touchWall(Facing.RIGHT)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            x.kickLocked shouldBe false

            hold()
            ticks(MegamanXValues.WALL_DELAY_AFTER_JUMP)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            x.kickLocked shouldBe true
        }

        it("jumps normally from the ground next to a wall") {
            overGround = true
            touchWall(Facing.RIGHT)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()

            body.physics.velocity.y shouldBe (MegamanXValues.JUMP_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
            x.kickLocked shouldBe false
        }
    }

    describe("the air dash") {
        fun startDash(vararg extra: Pair<MegaControllerButton, ButtonStatus>) {
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED, *extra)
            tick()
        }

        it("starts on a new press of A in the air and hovers first") {
            body.physics.velocity.set(2f, -3f)
            startDash()

            active(BehaviorType.AIR_DASHING) shouldBe true
            body.physics.gravityOn shouldBe false
            body.physics.velocity.x shouldBe 0f
            body.physics.velocity.y shouldBe 0f
        }

        it("moves at the dash speed after the hover") {
            startDash()
            hold()
            ticks(MegamanXValues.AIR_DASH_HOVER_TIME + DELTA)

            body.physics.velocity.x shouldBe (MegamanXValues.AIR_DASH_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
            body.physics.velocity.y shouldBe 0f
        }

        it("ends after the hover and the dash time, with no end bump") {
            startDash()
            hold()
            ticks(MegamanXValues.AIR_DASH_HOVER_TIME + MegamanXValues.AIR_DASH_TIME + 2 * DELTA)

            active(BehaviorType.AIR_DASHING) shouldBe false
            body.physics.gravityOn shouldBe true
            body.preProcess()
            body.physics.velocity.x shouldBe 0f
        }

        it("starts on the dash button during a rising jump, and the jump ends") {
            overGround = true
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            overGround = false
            hold(MegaControllerButton.A to ButtonStatus.PRESSED, MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe true
            hold(MegaControllerButton.A to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.JUMPING) shouldBe false
        }

        it("starts on the dash button") {
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe true
        }

        it("starts on SELECT and on A, whatever the select action is") {
            x.services.players.settings(PlayerSlot(0)).selectButtonAction = SelectButtonAction.AIR_DASH
            startDash()
            active(BehaviorType.AIR_DASHING) shouldBe true
        }

        it("does not start during a push-off") {
            startSlideOnLeftWall()
            ticks(MegamanXValues.WALL_GRAB_TIME + DELTA)
            hold()
            tick()
            x.pushingOff shouldBe true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe false
        }

        it("does not start on the ground") {
            overGround = true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe false
        }

        it("dashes once in the air, and again after a landing") {
            startDash()
            hold()
            ticks(MegamanXValues.AIR_DASH_HOVER_TIME + MegamanXValues.AIR_DASH_TIME + 2 * DELTA)
            startDash()
            active(BehaviorType.AIR_DASHING) shouldBe false

            overGround = true
            hold()
            tick()
            overGround = false
            startDash()
            active(BehaviorType.AIR_DASHING) shouldBe true
        }

        it("faces the held side at the start") {
            hold(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.LEFT to ButtonStatus.PRESSED, MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            x.facing shouldBe Facing.LEFT
            ticks(MegamanXValues.AIR_DASH_HOVER_TIME + DELTA)
            (body.physics.velocity.x < -MegamanXValues.AIR_DASH_SPEED * ConstVals.PPM) shouldBe true
        }

        it("ends on the opposite side input") {
            startDash()
            hold(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe false
        }

        it("ignores the run input during the dash") {
            startDash()
            ticks(MegamanXValues.AIR_DASH_HOVER_TIME + DELTA)
            hold(MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED)
            tick()
            body.physics.velocity.x shouldBe (MegamanXValues.AIR_DASH_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }
    }

    describe("the ground dash") {
        fun speedX() = body.physics.velocity.x / ConstVals.PPM

        it("starts on R on the ground, holds still for the start delay, then moves at the start speed") {
            overGround = true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.GROUND_DASHING) shouldBe true
            speedX() shouldBe 0f

            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED)
            ticks(MegamanXValues.DASH_START_DELAY)
            tick()
            (speedX() > MegamanXValues.DASH_SPEED) shouldBe true
            (speedX() <= MegamanXValues.DASH_START_SPEED) shouldBe true
        }

        it("slows to the dash speed and keeps it while R is held") {
            overGround = true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED)
            ticks(MegamanXValues.DASH_START_DELAY + 0.4f)
            speedX() shouldBe (MegamanXValues.DASH_SPEED plusOrMinus TOLERANCE)
        }

        it("slides to a stop after the dash time and then ends") {
            overGround = true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED)
            ticks(MegamanXValues.DASH_START_DELAY + MegamanXValues.DASH_TIME + 2 * DELTA)
            (speedX() < MegamanXValues.DASH_SPEED) shouldBe true
            ticks(MegamanXValues.DASH_END_TIME + DELTA)
            active(BehaviorType.GROUND_DASHING) shouldBe false
        }

        it("goes into the end slide when no dash input is held") {
            overGround = true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold()
            ticks(MegamanXValues.DASH_START_DELAY + 2 * DELTA)
            (speedX() < MegamanXValues.DASH_SPEED) shouldBe true
        }

        it("goes into the end slide on the opposite side, with no turn") {
            overGround = true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED)
            ticks(MegamanXValues.DASH_START_DELAY + DELTA)
            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED, MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED, MegaControllerButton.LEFT to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.GROUND_DASHING) shouldBe true
            x.groundDashJumpable shouldBe false
            x.facing shouldBe Facing.RIGHT
            (speedX() < MegamanXValues.DASH_SPEED) shouldBe true
        }

        it("does not end the end slide on the forward side") {
            overGround = true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold()
            ticks(MegamanXValues.DASH_START_DELAY + 2 * DELTA)
            hold(MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.GROUND_DASHING) shouldBe true
        }

        it("starts a new dash on R during the end slide") {
            overGround = true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold()
            ticks(MegamanXValues.DASH_START_DELAY + 2 * DELTA)
            x.groundDashJumpable shouldBe false
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            x.groundDashJumpable shouldBe true
        }

        it("uses up the air dash when X dashes off a ledge") {
            overGround = true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED)
            ticks(MegamanXValues.DASH_START_DELAY + DELTA)
            overGround = false
            tick()
            active(BehaviorType.GROUND_DASHING) shouldBe false
            x.airDashUsed shouldBe true
        }

        it("does not start on DOWN + A") {
            overGround = true
            hold(MegaControllerButton.DOWN to ButtonStatus.PRESSED, MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.GROUND_DASHING) shouldBe false
        }

        it("does not start in the air") {
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.GROUND_DASHING) shouldBe false
        }

        it("gives a dash jump: the dash air speed and no air dash after it") {
            overGround = true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED)
            ticks(MegamanXValues.DASH_START_DELAY + DELTA)
            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED, MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.JUMPING) shouldBe true
            x.dashSpeedInAir shouldBe true
            x.airDashUsed shouldBe true

            overGround = false
            hold(MegaControllerButton.A to ButtonStatus.PRESSED, MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.A to ButtonStatus.PRESSED, MegaControllerButton.RIGHT to ButtonStatus.PRESSED)
            tick()
            speedX() shouldBe (MegamanXValues.DASH_AIR_SPEED plusOrMinus TOLERANCE)

            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe false
        }

        it("goes into the walk start when the dash ends with the side held") {
            overGround = true
            hold(MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED, MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.RIGHT to ButtonStatus.PRESSED, MegaControllerButton.SELECT to ButtonStatus.PRESSED)
            var guard = 0
            while (active(BehaviorType.GROUND_DASHING) && guard++ < 200) tick()
            active(BehaviorType.GROUND_DASHING) shouldBe false
            body.preProcess()
            speedX() shouldBe (MegamanXValues.WALK_START_SPEED plusOrMinus TOLERANCE)
        }

        it("keeps the side speed on the takeoff frame of a dash jump") {
            overGround = true
            hold(MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED, MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.RIGHT to ButtonStatus.PRESSED, MegaControllerButton.SELECT to ButtonStatus.PRESSED)
            ticks(MegamanXValues.DASH_START_DELAY + DELTA)
            hold(
                MegaControllerButton.RIGHT to ButtonStatus.PRESSED,
                MegaControllerButton.SELECT to ButtonStatus.PRESSED,
                MegaControllerButton.A to ButtonStatus.JUST_PRESSED
            )
            tick()
            overGround = false
            hold(
                MegaControllerButton.RIGHT to ButtonStatus.PRESSED,
                MegaControllerButton.SELECT to ButtonStatus.PRESSED,
                MegaControllerButton.A to ButtonStatus.PRESSED
            )
            tick()
            body.preProcess()
            speedX() shouldBe (MegamanXValues.DASH_AIR_SPEED plusOrMinus TOLERANCE)
        }

        it("gives a plain kick after a dash jump without the dash air speed") {
            x.dashSpeedInAir = true
            x.airDashUsed = true
            touchWall(Facing.RIGHT)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            x.dashSpeedInAir shouldBe false
            x.airDashUsed shouldBe false
            speedX() shouldBe (-MegamanXValues.WALL_KICK_AWAY_SPEED plusOrMinus TOLERANCE)
        }

        it("gives a normal jump without the dash") {
            overGround = true
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            x.dashSpeedInAir shouldBe false
        }

        it("ends the dash air speed on landing") {
            x.dashSpeedInAir = true
            overGround = true
            tick()
            x.dashSpeedInAir shouldBe false
        }

        it("gives a dash wall kick with R held") {
            touchWall(Facing.RIGHT)
            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED, MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            speedX() shouldBe (-MegamanXValues.DASH_WALL_KICK_AWAY_SPEED plusOrMinus TOLERANCE)
            x.dashSpeedInAir shouldBe true
        }
    }

    describe("the landing") {
        it("starts the walk start again when X lands with a side held") {
            x.getComponent(RunComponent::class)!!.runTime = 1f
            tick()
            overGround = true
            tick()
            x.getComponent(RunComponent::class)!!.runTime shouldBe 0f
        }
    }

    describe("the R air dash") {
        it("starts faster on R than on A, then slows to the air dash speed") {
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold()
            ticks(MegamanXValues.AIR_DASH_HOVER_TIME + DELTA)
            (body.physics.velocity.x > MegamanXValues.AIR_DASH_SPEED * ConstVals.PPM) shouldBe true
            ticks(0.25f)
            body.physics.velocity.x shouldBe (MegamanXValues.AIR_DASH_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }
    }

    describe("the animation keys") {
        it("shows the slide, the kick and the dash") {
            startSlideOnLeftWall()
            x.animationKey() shouldBe "wallslide"

            hold(MegaControllerButton.LEFT to ButtonStatus.PRESSED, MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            tick()
            x.animationKey() shouldBe "walljump"

            leaveWalls()
            hold()
            ticks(MegamanXValues.WALL_KICK_LOCK_TIME + DELTA)
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            x.animationKey() shouldBe "dash"
        }
    }

    describe("the press frame") {
        it("keeps an A press on the landing frame for one frame") {
            x.airDashUsed = true
            tick()
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick { overGround = true }
            active(BehaviorType.JUMPING) shouldBe false

            hold(MegaControllerButton.A to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.JUMPING) shouldBe true
        }

        it("does not keep an A press from a frame with no landing") {
            x.airDashUsed = true
            tick()
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.A to ButtonStatus.PRESSED)
            tick { overGround = true }
            tick()
            active(BehaviorType.JUMPING) shouldBe false
        }

        it("does not keep an A press after a wall touch in the air") {
            x.airDashUsed = true
            tick()
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick { touchWall(Facing.RIGHT) }
            hold(MegaControllerButton.A to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.JUMPING) shouldBe false
        }

        it("keeps a dash press on the landing frame for one frame") {
            x.airDashUsed = true
            tick()
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick { overGround = true }

            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.GROUND_DASHING) shouldBe true
        }

        it("jumps with DOWN held on the ground") {
            overGround = true
            hold(MegaControllerButton.DOWN to ButtonStatus.PRESSED, MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.JUMPING) shouldBe true
        }

        it("moves on the press frame of a side") {
            overGround = true
            hold(MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED)
            tick()
            body.physics.velocity.x shouldBe (MegamanXValues.WALK_START_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("turns on the press frame of the opposite side") {
            overGround = true
            hold(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
            tick()
            hold(MegaControllerButton.LEFT to ButtonStatus.PRESSED, MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED)
            tick()
            x.facing shouldBe Facing.RIGHT
            (body.physics.velocity.x > 0f) shouldBe true
        }
    }

    describe("the move ends") {
        it("keeps the side speed on the frame that the air dash ends with a side held") {
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.RIGHT to ButtonStatus.PRESSED)
            var frames = 0
            while (active(BehaviorType.AIR_DASHING) && frames++ < 100) tick()
            body.preProcess()
            body.physics.velocity.x shouldBe (MegamanXValues.WALK_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("keeps the side speed on the frame that the wall slide ends at the wall's end, with the wall side held") {
            startSlideOnLeftWall()
            ticks(MegamanXValues.WALL_GRAB_TIME + DELTA)
            leaveWalls()
            tick()
            active(BehaviorType.WALL_SLIDING) shouldBe false
            body.preProcess()
            body.physics.velocity.x shouldBe (-MegamanXValues.WALK_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("keeps a falling speed at the jump end") {
            overGround = true
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            overGround = false
            body.physics.velocity.y = -ConstVals.PPM.toFloat()
            hold(MegaControllerButton.A to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.JUMPING) shouldBe false
            body.physics.velocity.y shouldBe -ConstVals.PPM.toFloat()
        }

        it("stops the rise at the jump end on a release") {
            overGround = true
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            overGround = false
            hold()
            tick()
            body.physics.velocity.y shouldBe 0f
        }

        it("moves slower than the walk after a kick and a push-off, and faster after a dash kick") {
            (MegamanXValues.WALL_KICK_AWAY_SPEED < MegamanXValues.WALK_SPEED) shouldBe true
            (MegamanXValues.PUSH_OFF_SPEED < MegamanXValues.WALK_SPEED) shouldBe true
            (MegamanXValues.DASH_WALL_KICK_AWAY_SPEED > MegamanXValues.WALK_SPEED) shouldBe true
        }
    }

    describe("the 081d animation keys") {
        it("shows the kick pose on the kick frame") {
            startSlideOnLeftWall()
            hold(MegaControllerButton.LEFT to ButtonStatus.PRESSED, MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            x.animationKey() shouldBe "walljump"
        }

        it("keeps the kick pose after the lock while the kick still rises, then falls") {
            touchWall(Facing.RIGHT)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            leaveWalls()
            hold(MegaControllerButton.A to ButtonStatus.PRESSED)
            ticks(MegamanXValues.WALL_KICK_LOCK_TIME + DELTA)
            x.kickLocked shouldBe false
            active(BehaviorType.JUMPING) shouldBe true
            x.animationKey() shouldBe "walljump"

            hold()
            tick()
            x.animationKey() shouldBe "fall"
        }

        it("shows the brake pose in the end slide of the ground dash") {
            overGround = true
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            hold()
            ticks(MegamanXValues.DASH_START_DELAY + DELTA)
            tick()
            active(BehaviorType.GROUND_DASHING) shouldBe true
            x.animationKey() shouldBe "dash_end"
        }
    }

    describe("the landing probe") {
        fun blockFixture(blockBody: Body, width: Float, height: Float): Fixture {
            val block = mockk<Block>(relaxed = true)
            every { block.body } returns blockBody
            val fixture = Fixture(blockBody, FixtureType.BLOCK, GameRectangle(0f, 0f, width, height))
            fixture.putProperty(ConstKeys.ENTITY, block)
            return fixture
        }

        fun floorBelow(gap: Float, fallSpeed: Float = 5f) {
            body.set(0f, 64f, MegamanXValues.BODY_WIDTH * ConstVals.PPM, MegamanXValues.BODY_HEIGHT * ConstVals.PPM)
            body.preProcess()
            val container = SimpleGridWorldContainer(ConstVals.PPM)
            val floorBody = Body(BodyType.STATIC)
            floorBody.set(-128f, 64f - 0.05f * ConstVals.PPM - gap - 32f, 320f, 32f)
            val floor = blockFixture(floorBody, 320f, 32f)
            container.addFixture(floor)
            services.levelState.worldContainer = container
            body.physics.velocity.y = -fallSpeed * ConstVals.PPM
            tick()
        }

        it("sees a slow landing from the top of the arc") {
            floorBelow(0.4f, fallSpeed = 0f)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe false
        }

        it("probes along the gravity in LEFT gravity") {
            x.direction = Direction.LEFT
            body.set(0f, 64f, MegamanXValues.BODY_WIDTH * ConstVals.PPM, MegamanXValues.BODY_HEIGHT * ConstVals.PPM)
            body.preProcess()
            val feetEdge = body.getX() + body.getWidth() / 2f + MegamanXValues.BODY_HEIGHT * ConstVals.PPM / 2f + 0.05f * ConstVals.PPM
            val container = SimpleGridWorldContainer(ConstVals.PPM)
            val wallBody = Body(BodyType.STATIC)
            wallBody.set(feetEdge + 0.4f, -64f, 32f, 320f)
            container.addFixture(blockFixture(wallBody, 32f, 320f))
            services.levelState.worldContainer = container
            body.physics.velocity.setZero()
            tick()

            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe false
        }

        it("gives a ground jump, not an air dash, for an A press when X lands in the frame") {
            floorBelow(1f)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick { overGround = true }
            active(BehaviorType.AIR_DASHING) shouldBe false

            hold(MegaControllerButton.A to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.JUMPING) shouldBe true
        }

        it("gives a ground dash for a SELECT press when X lands in the frame") {
            floorBelow(1f)
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick { overGround = true }
            active(BehaviorType.AIR_DASHING) shouldBe false

            hold(MegaControllerButton.SELECT to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.GROUND_DASHING) shouldBe true
        }

        it("starts the air dash 1 frame later when the landing does not come") {
            floorBelow(1f)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe false

            hold(MegaControllerButton.A to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe true
        }

        it("starts the air dash at once with no floor near") {
            floorBelow(200f)
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe true
        }
    }
})
