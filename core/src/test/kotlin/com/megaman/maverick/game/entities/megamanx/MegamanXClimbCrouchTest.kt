package com.megaman.maverick.game.entities.megamanx

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.Points
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.players.playerCanMove
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyKeys
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

class MegamanXClimbCrouchTest : DescribeSpec({

    val ppm = ConstVals.PPM.toFloat()

    lateinit var x: MegamanX
    lateinit var services: MegaServices
    lateinit var body: Body
    lateinit var behaviors: BehaviorsComponent
    lateinit var controllerSystem: ControllerSystem
    var frame = InputFrame.RELEASED
    var overGround = false

    val order = listOf(
        BehaviorType.WALL_SLIDING, BehaviorType.GROUND_DASHING, BehaviorType.JUMPING, BehaviorType.AIR_DASHING,
        BehaviorType.CROUCHING, BehaviorType.CLIMBING
    )

    fun hold(vararg pairs: Pair<MegaControllerButton, ButtonStatus>) {
        frame = InputFrame.RELEASED
        pairs.forEach { (button, status) -> frame = frame.with(button, status) }
    }

    fun tick(delta: Float = DELTA) {
        controllerSystem.update(delta)
        order.forEach { behaviors.getBehavior(it)!!.update(delta) }
        body.preProcess()
        x.updateLadderPose(delta)
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

    fun centerX() = body.getX() + body.getWidth() / 2f

    fun onGround(on: Boolean) {
        overGround = on
        body.setBodySense(BodySense.FEET_ON_GROUND, on)
    }

    fun place(tilesX: Float, tilesY: Float) {
        body.setPosition(tilesX * ppm, tilesY * ppm)
        body.preProcess()
    }

    fun touchLadder(head: Boolean, feet: Boolean) {
        body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, head)
        body.setBodySense(BodySense.FEET_TOUCHING_LADDER, feet)
    }

    fun grabFromFloor() {
        place(4.2f, 0f)
        onGround(true)
        touchLadder(head = true, feet = true)
        hold(MegaControllerButton.UP to ButtonStatus.PRESSED)
        tick()
        onGround(false)
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
        x.addComponent(WeaponWindupComponent())
        x.addComponent(PostActionMomentumComponent(0f, 0f, { 0f }, { overGround }, { false }, { false }))
        x.addComponent(AudioComponent())
        x.addComponent(PointsComponent(ConstKeys.HEALTH pairTo Points(0, ConstVals.MAX_HEALTH, ConstVals.MAX_HEALTH)))
        behaviors = x.defineBehaviorsComponent()
        x.addComponent(behaviors)

        services.players.register(PlayerSlot(0), x)
        x.addComponent(x.defineControllerComponent().also { it.input = InputSource { frame } })

        controllerSystem = ControllerSystem()
        controllerSystem.add(x)

        val ladderBody = Body(BodyType.ABSTRACT).also { it.set(4f * ppm, 0f, 1f * ppm, 6f * ppm) }
        val ladder = mockk<IBodyEntity>(relaxed = true)
        every { ladder.body } returns ladderBody
        body.putProperty(ConstKeys.LADDER, ladder)
    }

    afterEach { unmockkAll() }

    describe("the ladder grab") {
        it("grabs from the floor with UP, snaps to the ladder center, and turns gravity off") {
            grabFromFloor()
            active(BehaviorType.CLIMBING) shouldBe true
            centerX() shouldBe (4.5f * ppm plusOrMinus TOLERANCE)
            body.physics.gravityOn shouldBe false
            x.animationKey() shouldBe "climb"
        }

        it("grabs from the top with DOWN in the climb-down pose, and holds still for its time") {
            place(4.2f, 6f)
            onGround(true)
            touchLadder(head = false, feet = true)
            hold(MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            tick()
            onGround(false)

            active(BehaviorType.CLIMBING) shouldBe true
            active(BehaviorType.CROUCHING) shouldBe false
            x.animationKey() shouldBe "climb_start"
            tick()
            body.physics.velocity.y shouldBe 0f

            ticks(MegamanXValues.CLIMB_START_TIME)
            tick()
            x.animationKey() shouldBe "climb"
            body.physics.velocity.y shouldBe (-MegamanXValues.CLIMB_SPEED * ppm plusOrMinus TOLERANCE)
        }

        it("grabs in the air with UP and gives a new air dash") {
            x.airDashUsed = true
            x.dashSpeedInAir = true
            place(4.2f, 2f)
            touchLadder(head = true, feet = true)
            hold(MegaControllerButton.UP to ButtonStatus.PRESSED)
            tick()

            active(BehaviorType.CLIMBING) shouldBe true
            x.airDashUsed shouldBe false
            x.dashSpeedInAir shouldBe false
        }
    }

    describe("the climb") {
        it("climbs at the X6 ladder speed") {
            grabFromFloor()
            tick()
            body.physics.velocity.y shouldBe (MegamanXValues.CLIMB_SPEED * ppm plusOrMinus TOLERANCE)
        }

        it("turns on the ladder with no side speed") {
            grabFromFloor()
            hold(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
            tick()

            x.facing shouldBe Facing.LEFT
            x.running shouldBe false
            body.physics.velocity.x shouldBe 0f
            active(BehaviorType.CLIMBING) shouldBe true
        }

        it("passes one-way blocks only while it climbs") {
            @Suppress("UNCHECKED_CAST")
            val pass = body.getProperty(BodyKeys.PASS_THROUGH_ONE_WAY) as () -> Boolean
            hold(MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            pass() shouldBe false

            grabFromFloor()
            pass() shouldBe true
        }
    }

    describe("the ladder exits") {
        it("climbs over the top in the finish pose, with a short movement lock") {
            grabFromFloor()
            place(4f, 5.8f)
            touchLadder(head = false, feet = true)
            tick()

            active(BehaviorType.CLIMBING) shouldBe false
            x.animationKey() shouldBe "climb_finish"
            x.playerCanMove shouldBe false

            ticks(MegamanXValues.CLIMB_FINISH_TIME + DELTA)
            x.playerCanMove shouldBe true
            x.ladderPose shouldBe null
        }

        it("leaves on the floor with DOWN") {
            grabFromFloor()
            hold(MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.CLIMBING) shouldBe true

            onGround(true)
            tick()
            active(BehaviorType.CLIMBING) shouldBe false
            x.ladderPose shouldBe null
        }

        it("does not grab again at the bottom while DOWN stays held, and crouches") {
            grabFromFloor()
            onGround(true)
            hold(MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.CLIMBING) shouldBe false

            ticks(0.2f)
            active(BehaviorType.CLIMBING) shouldBe false
            active(BehaviorType.CROUCHING) shouldBe true
        }

        it("does not grab with DOWN in the air") {
            place(4.2f, 5f)
            touchLadder(head = false, feet = true)
            hold(MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.CLIMBING) shouldBe false
        }

        it("lets go on A and does not jump") {
            grabFromFloor()
            hold(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()

            active(BehaviorType.CLIMBING) shouldBe false
            active(BehaviorType.JUMPING) shouldBe false
            body.physics.gravityOn shouldBe true
            x.playerCanMove shouldBe true
        }
    }

    describe("the crouch") {
        fun crouch() {
            place(2f, 0f)
            onGround(true)
            hold(MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            tick()
        }

        it("crouches at once on DOWN with a lower body on the same feet") {
            crouch()
            active(BehaviorType.CROUCHING) shouldBe true
            x.animationKey() shouldBe "crouch"
            body.getHeight() shouldBe (MegamanXValues.CROUCH_HEIGHT * ppm plusOrMinus TOLERANCE)
            body.getY() shouldBe (0f plusOrMinus TOLERANCE)
        }

        it("turns with no side speed") {
            crouch()
            hold(MegaControllerButton.DOWN to ButtonStatus.PRESSED, MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            tick()
            hold(MegaControllerButton.DOWN to ButtonStatus.PRESSED, MegaControllerButton.LEFT to ButtonStatus.PRESSED)
            tick()

            x.facing shouldBe Facing.LEFT
            x.running shouldBe false
            body.physics.velocity.x shouldBe 0f
            active(BehaviorType.CROUCHING) shouldBe true
        }

        it("rises for one frame on release, then stands at full height") {
            crouch()
            hold()
            tick()
            active(BehaviorType.CROUCHING) shouldBe true
            x.animationKey() shouldBe "crouch_rise"

            ticks(MegamanXValues.CROUCH_FRAME_TIME + DELTA)
            active(BehaviorType.CROUCHING) shouldBe false
            body.getHeight() shouldBe (MegamanXValues.BODY_HEIGHT * ppm plusOrMinus TOLERANCE)
        }

        it("jumps out of the crouch") {
            crouch()
            hold(MegaControllerButton.DOWN to ButtonStatus.PRESSED, MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.JUMPING) shouldBe true
            tick()
            active(BehaviorType.CROUCHING) shouldBe false
        }

        it("dashes out of the crouch") {
            crouch()
            hold(
                MegaControllerButton.DOWN to ButtonStatus.PRESSED,
                MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED
            )
            tick()
            active(BehaviorType.GROUND_DASHING) shouldBe true
            tick()
            active(BehaviorType.CROUCHING) shouldBe false
        }
    }
})
