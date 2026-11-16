package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.graphics.OrthographicCamera
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.BehaviorsSystem
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.camera.CameraRotation
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.locks.unlock
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.Points
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.enemies.MetaWheelF
import com.megaman.maverick.game.entities.megaman.behaviors.groundSlideBehavior
import com.megaman.maverick.game.entities.megaman.behaviors.jumpBehavior
import com.megaman.maverick.game.entities.megaman.components.GROUNDSLIDE_CROUCH_HEIGHT
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.PostActionMomentumComponent
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.TagsComponent
import com.megaman.maverick.game.entities.state.TeleportableComponent
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private const val DELTA = 0.05f
private const val TOLERANCE = 0.01f

class MegamanGroundSlideTest : DescribeSpec({

    val ppm = ConstVals.PPM.toFloat()
    val slideSpeed = MegamanValues.GROUND_SLIDE_VEL * ppm

    lateinit var megaman: Megaman
    lateinit var body: Body
    lateinit var behaviors: BehaviorsComponent
    lateinit var system: BehaviorsSystem
    lateinit var slide: IBehavior
    lateinit var jump: IBehavior
    lateinit var rotation: CameraRotation
    var frame = InputFrame.RELEASED
    var overGround = true

    fun press(vararg pairs: Pair<MegaControllerButton, ButtonStatus>) {
        frame = InputFrame.RELEASED
        pairs.forEach { (button, status) -> frame = frame.with(button, status) }
    }

    fun step(vararg pairs: Pair<MegaControllerButton, ButtonStatus>) {
        press(*pairs)
        system.update(DELTA)
        body.preProcess()
    }

    fun start() = step(MegaControllerButton.DOWN to ButtonStatus.PRESSED, MegaControllerButton.A to ButtonStatus.JUST_PRESSED)

    fun holdBoth() = step(MegaControllerButton.DOWN to ButtonStatus.PRESSED, MegaControllerButton.A to ButtonStatus.PRESSED)

    fun holdBothFor(seconds: Float) {
        var time = DELTA
        while (time < seconds - TOLERANCE) {
            holdBoth()
            time += DELTA
        }
    }

    fun setActive(type: BehaviorType) {
        val behavior = mockk<IBehavior>(relaxed = true)
        every { behavior.isActive() } returns true
        behaviors.addBehavior(type, behavior)
    }

    fun momentum() = megaman.getComponent(PostActionMomentumComponent::class)!!

    fun turn(direction: Direction) {
        megaman.direction = direction
        megaman.unlock(MegaLockGroup.MOVEMENT, MegaLockReason.CAMERA_ROTATION)
    }

    fun damageableSize(): Pair<Float, Float> {
        val fixture = body.fixtures.get(FixtureType.DAMAGEABLE).first() as Fixture
        val rect = fixture.rawShape as GameRectangle
        return rect.getWidth() / ppm to rect.getHeight() / ppm
    }

    fun hurt() {
        val damager = MetaWheelF(megaman.services).also { it.addComponent(TagsComponent(MetaWheelF.TAG)) }
        megaman.takeDamageFrom(damager) shouldBe true
    }

    beforeEach {
        frame = InputFrame.RELEASED
        overGround = true
        rotation = CameraRotation()

        val services = testServices(
            engine = mockk<GameEngine>(relaxed = true),
            gameCamera = CameraView(OrthographicCamera(), rotation)
        )
        services.levelState.currentLevel = LevelDefinition.DESERT_MAN

        megaman = Megaman(services)
        megaman.existenceState = EntityExistenceState.LIVE
        megaman.weaponsHandler = mockk(relaxed = true)
        megaman.jetpackHandler = mockk(relaxed = true)
        megaman.currentWeapon = MegamanWeapons.MEGA_BUSTER
        megaman.shooting = false
        megaman.facing = Facing.RIGHT
        megaman.aButtonTask = AButtonTask.JUMP
        megaman.jumpVel = MegamanValues.JUMP_VEL
        megaman.wallJumpVel = MegamanValues.WALL_JUMP_VEL

        behaviors = BehaviorsComponent()
        megaman.addComponent(behaviors)
        megaman.addRuleComponents()
        megaman.addComponent(megaman.defineBodyComponent())
        body = megaman.body
        body.putProperty(ConstKeys.BODY_OVER_GROUND, { overGround })
        body.setBodySense(BodySense.FEET_ON_GROUND, true)
        megaman.addComponent(TeleportableComponent())
        megaman.addComponent(RunComponent())
        megaman.addComponent(megaman.defineMomentumComponent())
        megaman.addComponent(AudioComponent())
        megaman.addComponent(
            PointsComponent(ConstKeys.HEALTH pairTo Points(0, ConstVals.MAX_HEALTH, ConstVals.MAX_HEALTH))
        )
        services.players.register(PlayerSlot(0), megaman)
        megaman.addComponent(ControllerComponent().also { it.input = InputSource { frame } })

        jump = megaman.jumpBehavior()
        slide = megaman.groundSlideBehavior()
        behaviors.addBehavior(BehaviorType.JUMPING, jump)
        behaviors.addBehavior(BehaviorType.GROUND_SLIDING, slide)

        system = BehaviorsSystem()
        system.add(megaman)
        step()
        step()
    }

    afterEach { unmockkAll() }

    describe("the start") {
        it("starts on the ground with DOWN held and A just pressed") {
            start()
            slide.isActive() shouldBe true
        }

        it("does not jump on DOWN and A") {
            start()
            jump.isActive() shouldBe false
        }

        it("does not start with A held from before") {
            holdBoth()
            slide.isActive() shouldBe false
        }

        it("does not start with A alone, which jumps") {
            step(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            slide.isActive() shouldBe false
            jump.isActive() shouldBe true
        }

        it("does not start in the air") {
            overGround = false
            body.setBodySense(BodySense.FEET_ON_GROUND, false)
            start()
            slide.isActive() shouldBe false
        }

        it("does not start over the ground with the feet off the ground") {
            body.setBodySense(BodySense.FEET_ON_GROUND, false)
            start()
            slide.isActive() shouldBe false
        }

        it("does not start on sand") {
            body.setBodySense(BodySense.FEET_ON_SAND, true)
            start()
            slide.isActive() shouldBe false
        }

        it("does not start while jetpacking") {
            setActive(BehaviorType.JETPACKING)
            start()
            slide.isActive() shouldBe false
        }

        it("does not start while Needle Spin shoots") {
            megaman.currentWeapon = MegamanWeapons.NEEDLE_SPIN
            megaman.shooting = true
            start()
            slide.isActive() shouldBe false
        }

        it("starts with Needle Spin when it does not shoot") {
            megaman.currentWeapon = MegamanWeapons.NEEDLE_SPIN
            start()
            slide.isActive() shouldBe true
        }

        it("does not start in a camera turn") {
            rotation.start(Direction.LEFT, 1f)
            start()
            slide.isActive() shouldBe false
        }

        it("does not start out of play") {
            megaman.getComponent(PlayPhaseComponent::class)!!.phase = PlayPhase.PRE_PLAY
            start()
            slide.isActive() shouldBe false
        }

        it("does not start when he is destroyed") {
            megaman.existenceState = EntityExistenceState.DESTROY_QUEUED
            start()
            slide.isActive() shouldBe false
        }

        it("does not start with the movement locked") {
            megaman.lock(MegaLockGroup.MOVEMENT, MegaLockReason.CAMERA_ROTATION)
            start()
            slide.isActive() shouldBe false
        }

        it("does not start while damaged") {
            hurt()
            start()
            slide.isActive() shouldBe false
        }
    }

    describe("the times") {
        it("ends at the max time with DOWN and A held") {
            start()
            holdBothFor(MegamanValues.GROUND_SLIDE_MAX_TIME - DELTA)
            slide.isActive() shouldBe true
            holdBoth()
            holdBoth()
            holdBoth()
            slide.isActive() shouldBe false
        }

        it("goes on inside the min time with DOWN and A released") {
            start()
            step()
            slide.isActive() shouldBe true
        }

        it("ends after the min time when DOWN is released") {
            start()
            holdBothFor(MegamanValues.GROUND_SLIDE_MIN_TIME + DELTA)
            slide.isActive() shouldBe true
            step(MegaControllerButton.A to ButtonStatus.PRESSED)
            slide.isActive() shouldBe false
        }

        it("ends after the min time when A is released") {
            start()
            holdBothFor(MegamanValues.GROUND_SLIDE_MIN_TIME + DELTA)
            step(MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            slide.isActive() shouldBe false
        }

        it("ends after the min time when the gravity is not the start gravity") {
            start()
            holdBothFor(MegamanValues.GROUND_SLIDE_MIN_TIME + DELTA)
            turn(Direction.DOWN)
            holdBoth()
            slide.isActive() shouldBe false
        }

        it("goes on inside the min time after a gravity change") {
            start()
            turn(Direction.DOWN)
            holdBoth()
            slide.isActive() shouldBe true
        }

        it("ends off the ground") {
            start()
            overGround = false
            holdBoth()
            slide.isActive() shouldBe false
        }

        it("ends on damage") {
            start()
            hurt()
            holdBoth()
            slide.isActive() shouldBe false
        }

        it("waits for the cooldown after the end before a new start") {
            start()
            overGround = false
            holdBoth()
            overGround = true
            start()
            slide.isActive() shouldBe false
            start()
            slide.isActive() shouldBe true
        }
    }

    describe("the low ceiling") {
        it("stays on past the max time with DOWN released while the head touches a block") {
            start()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
            var time = 0f
            while (time < MegamanValues.GROUND_SLIDE_MAX_TIME * 2f) {
                step()
                time += DELTA
            }
            slide.isActive() shouldBe true
        }

        it("ends once the head is clear after the max time") {
            start()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
            holdBothFor(MegamanValues.GROUND_SLIDE_MAX_TIME + DELTA)
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, false)
            step()
            slide.isActive() shouldBe false
        }

        it("does not keep the slide on off the ground") {
            start()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
            overGround = false
            step()
            slide.isActive() shouldBe false
        }
    }

    describe("the speed") {
        it("moves 10 tiles per second the way he faces") {
            start()
            body.physics.velocity.x shouldBe (slideSpeed plusOrMinus TOLERANCE)
        }

        it("turns with the facing") {
            start()
            megaman.facing = Facing.LEFT
            holdBoth()
            body.physics.velocity.x shouldBe (-slideSpeed plusOrMinus TOLERANCE)
        }

        it("moves 6 tiles per second in water") {
            body.setBodySense(BodySense.IN_WATER, true)
            start()
            body.physics.velocity.x shouldBe (MegamanValues.WATER_GROUND_SLIDE_VEL * ppm plusOrMinus TOLERANCE)
        }

        it("scales with the movement scalar") {
            megaman.addComponent(AnimationsComponent())
            megaman.movementScalar = 0.5f
            start()
            body.physics.velocity.x shouldBe (slideSpeed * 0.5f plusOrMinus TOLERANCE)
        }

        it("is 1.5 times faster with the ground slide boost") {
            megaman.services.gameState.addEnhancement(MegaEnhancement.GROUND_SLIDE_BOOST)
            start()
            body.physics.velocity.x shouldBe
                (slideSpeed * MegaEnhancement.GROUND_SLIDE_BOOST_SCALAR plusOrMinus TOLERANCE)
        }

        it("moves along the floor in each gravity direction") {
            val expected = mapOf(
                Direction.UP to (slideSpeed to 0f),
                Direction.DOWN to (-slideSpeed to 0f),
                Direction.LEFT to (0f to slideSpeed),
                Direction.RIGHT to (0f to -slideSpeed)
            )
            expected.forEach { (direction, speed) ->
                turn(direction)
                body.physics.velocity.setZero()
                start()
                body.physics.velocity.x shouldBe (speed.first plusOrMinus TOLERANCE)
                body.physics.velocity.y shouldBe (speed.second plusOrMinus TOLERANCE)
                step()
                step()
                overGround = false
                step()
                overGround = true
                step()
            }
        }

        it("does not set the speed while damaged") {
            start()
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
            body.physics.velocity.setZero()
            slide.act(DELTA)
            body.physics.velocity.x shouldBe (slideSpeed plusOrMinus TOLERANCE)
            hurt()
            body.physics.velocity.setZero()
            slide.act(DELTA)
            body.physics.velocity.x shouldBe 0f
        }
    }

    describe("the body") {
        it("is 0.9 tiles high and 1 wide in the slide, with no gravity") {
            start()
            (body.getHeight() / ppm) shouldBe (GROUNDSLIDE_CROUCH_HEIGHT plusOrMinus TOLERANCE)
            (body.getWidth() / ppm) shouldBe (1f plusOrMinus TOLERANCE)
            body.physics.gravity.y shouldBe 0f
        }

        it("turns the damage box to lie along the floor") {
            start()
            val (width, height) = damageableSize()
            width shouldBe (1.5f plusOrMinus TOLERANCE)
            height shouldBe (1f plusOrMinus TOLERANCE)
        }

        it("is 1.5 tiles high after the slide") {
            start()
            overGround = false
            step()
            (body.getHeight() / ppm) shouldBe (1.5f plusOrMinus TOLERANCE)
        }

        it("moves the body at the start for each gravity direction") {
            val expected = mapOf(
                (Direction.UP to Facing.RIGHT) to (0f to 0f),
                (Direction.DOWN to Facing.RIGHT) to (0f to 0.6f),
                (Direction.LEFT to Facing.RIGHT) to (0.5f to 0.1f),
                (Direction.LEFT to Facing.LEFT) to (0.5f to -0.1f),
                (Direction.RIGHT to Facing.RIGHT) to (-0.5f to -0.1f),
                (Direction.RIGHT to Facing.LEFT) to (-0.5f to 0.1f)
            )
            expected.forEach { (state, move) ->
                megaman.direction = state.first
                megaman.facing = state.second
                body.setPosition(10f * ppm, 10f * ppm)
                slide.init()
                (body.getX() / ppm - 10f) shouldBe (move.first plusOrMinus TOLERANCE)
                (body.getY() / ppm - 10f) shouldBe (move.second plusOrMinus TOLERANCE)
            }
        }

        it("moves the body down 0.35 tiles at the end only for a sideways gravity") {
            val expected = mapOf(
                Direction.UP to 0f,
                Direction.DOWN to 0f,
                Direction.LEFT to -0.35f,
                Direction.RIGHT to -0.35f
            )
            expected.forEach { (direction, move) ->
                megaman.direction = direction
                body.setPosition(10f * ppm, 10f * ppm)
                slide.end()
                (body.getX() / ppm - 10f) shouldBe (0f plusOrMinus TOLERANCE)
                (body.getY() / ppm - 10f) shouldBe (move plusOrMinus TOLERANCE)
            }
        }
    }

    describe("the end and the momentum") {
        it("keeps the speed at the end on the ground, clamped to the run speed") {
            start()
            holdBothFor(MegamanValues.GROUND_SLIDE_MIN_TIME + DELTA)
            step(MegaControllerButton.A to ButtonStatus.PRESSED)
            slide.isActive() shouldBe false
            body.physics.velocity.x shouldBe (MegamanValues.RUN_MAX_SPEED * ppm plusOrMinus TOLERANCE)
            momentum().timer.isFinished() shouldBe false
            momentum().pendingKill shouldBe false
        }

        it("keeps the speed at the max time end, clamped to the run speed") {
            start()
            holdBothFor(MegamanValues.GROUND_SLIDE_MAX_TIME + 2f * DELTA)
            slide.isActive() shouldBe false
            body.physics.velocity.x shouldBe (MegamanValues.RUN_MAX_SPEED * ppm plusOrMinus TOLERANCE)
        }

        it("asks for a momentum kill when it ends in the air") {
            start()
            overGround = false
            step()
            slide.isActive() shouldBe false
            momentum().pendingKill shouldBe true
            body.physics.velocity.x shouldBe (slideSpeed plusOrMinus TOLERANCE)
        }

        it("keeps the momentum in the slide and after it on ice") {
            body.setBodySense(BodySense.FEET_ON_ICE, true)
            start()
            momentum().timer.isFinished() shouldBe false
            holdBothFor(MegamanValues.GROUND_SLIDE_MAX_TIME + 2f * DELTA)
            slide.isActive() shouldBe false
            momentum().timer.isFinished() shouldBe false
            body.physics.velocity.x shouldBe (MegamanValues.RUN_MAX_SPEED * ppm plusOrMinus TOLERANCE)
        }

        it("stops the momentum on snow, and stops at the end") {
            body.setBodySense(BodySense.FEET_ON_SNOW, true)
            momentum().timer.reset()
            start()
            momentum().timer.isFinished() shouldBe true
            holdBothFor(MegamanValues.GROUND_SLIDE_MIN_TIME + DELTA)
            step(MegaControllerButton.A to ButtonStatus.PRESSED)
            slide.isActive() shouldBe false
            body.physics.velocity.x shouldBe 0f
        }
    }

    describe("the slide-jump cut") {
        it("cuts a jump inside the min time: the slide stays on and the up speed is lost") {
            start()
            step(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            jump.isActive() shouldBe true
            slide.isActive() shouldBe true
            body.physics.velocity.y shouldBe 0f
            step(MegaControllerButton.A to ButtonStatus.PRESSED)
            jump.isActive() shouldBe false
        }

        it("keeps the jump speed after the min time, when the DOWN release ends the slide") {
            start()
            holdBothFor(MegamanValues.GROUND_SLIDE_MIN_TIME + DELTA)
            step(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            jump.isActive() shouldBe true
            slide.isActive() shouldBe false
            body.physics.velocity.y shouldBe (MegamanValues.JUMP_VEL * ppm plusOrMinus TOLERANCE)
        }
    }
})
