package com.megaman.maverick.game.entities.megamanx

import com.badlogic.gdx.Input
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
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
import io.mockk.mockk
import io.mockk.unmockkAll

private const val DELTA = 0.02f
private const val TOLERANCE = 0.001f

class MegamanXMovesTest : DescribeSpec({

    lateinit var x: MegamanX
    lateinit var body: Body
    lateinit var behaviors: BehaviorsComponent
    lateinit var controllerSystem: ControllerSystem
    var frame = InputFrame.RELEASED
    var overGround = false

    val order = listOf(BehaviorType.WALL_SLIDING, BehaviorType.JUMPING, BehaviorType.AIR_DASHING)

    fun hold(vararg pairs: Pair<MegaControllerButton, ButtonStatus>) {
        frame = InputFrame.RELEASED
        pairs.forEach { (button, status) -> frame = frame.with(button, status) }
    }

    fun tick(delta: Float = DELTA) {
        controllerSystem.update(delta)
        order.forEach { behaviors.getBehavior(it)!!.update(delta) }
        x.kickLockTimer.update(delta)
        x.pushOffTimer.update(delta)
        x.wallDelayTimer.update(delta)
        if (overGround) x.airDashUsed = false
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

        val services = testServices(engine = mockk<GameEngine>(relaxed = true))
        services.levelState.currentLevel = LevelDefinition.X6_INTRO

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

        it("pushes off the wall when the wall side is released") {
            startSlideOnLeftWall()
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
            val field = MegamanX::class.java.getDeclaredField("damageTimer").apply { isAccessible = true }
            (field.get(x) as com.maverick.game.engine.common.time.Timer).reset()
            tick()

            active(BehaviorType.WALL_SLIDING) shouldBe false
            x.pushingOff shouldBe false
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
            hold(MegaControllerButton.A to ButtonStatus.PRESSED, MegaControllerButton.R to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe true
            hold(MegaControllerButton.A to ButtonStatus.PRESSED)
            tick()
            active(BehaviorType.JUMPING) shouldBe false
        }

        it("starts on the dash button") {
            hold(MegaControllerButton.R to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe true
        }

        it("starts on SELECT, not on A, when the select action is air dash") {
            x.services.players.settings(PlayerSlot(0)).selectButtonAction = SelectButtonAction.AIR_DASH
            startDash()
            active(BehaviorType.AIR_DASHING) shouldBe false
            hold(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            tick()
            active(BehaviorType.AIR_DASHING) shouldBe true
        }

        it("does not start on the ground") {
            overGround = true
            hold(MegaControllerButton.R to ButtonStatus.JUST_PRESSED)
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
            hold(MegaControllerButton.LEFT to ButtonStatus.PRESSED, MegaControllerButton.R to ButtonStatus.JUST_PRESSED)
            tick()
            x.facing shouldBe Facing.LEFT
            ticks(MegamanXValues.AIR_DASH_HOVER_TIME + DELTA)
            body.physics.velocity.x shouldBe (-MegamanXValues.AIR_DASH_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
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
            hold(MegaControllerButton.R to ButtonStatus.JUST_PRESSED)
            tick()
            x.animationKey() shouldBe "dash"
        }
    }

    describe("the dash button") {
        it("is bound to U by default") {
            MegaControllerButton.R.defaultKeyboardKey shouldBe Input.Keys.U
        }
    }
})
