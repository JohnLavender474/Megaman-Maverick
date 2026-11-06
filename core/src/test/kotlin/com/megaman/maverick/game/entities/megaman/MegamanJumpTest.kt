package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.behaviors.jumpBehavior
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.state.PostActionMomentumComponent
import com.megaman.maverick.game.entities.state.TeleportableComponent
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private const val TOLERANCE = 0.001f

class MegamanJumpTest : DescribeSpec({

    lateinit var megaman: Megaman
    lateinit var body: Body
    lateinit var behaviors: BehaviorsComponent
    lateinit var jump: IBehavior
    var frame = InputFrame.RELEASED
    var overGround = true

    fun press(status: ButtonStatus, vararg extra: MegaControllerButton) {
        frame = InputFrame.RELEASED.with(MegaControllerButton.A, status)
        extra.forEach { frame = frame.with(it, ButtonStatus.PRESSED) }
    }

    fun setBehaviorActive(type: BehaviorType) {
        val behavior = mockk<IBehavior>(relaxed = true)
        every { behavior.isActive() } returns true
        behaviors.addBehavior(type, behavior)
    }

    beforeEach {
        frame = InputFrame.RELEASED
        overGround = true

        val services = testServices(engine = mockk<GameEngine>(relaxed = true))
        services.levelState.currentLevel = LevelDefinition.DESERT_MAN

        megaman = Megaman(services)
        megaman.existenceState = EntityExistenceState.LIVE
        megaman.weaponsHandler = mockk(relaxed = true)
        megaman.currentWeapon = MegamanWeapon.MEGA_BUSTER
        megaman.facing = Facing.RIGHT
        megaman.aButtonTask = AButtonTask.JUMP
        megaman.jumpVel = MegamanValues.JUMP_VEL
        megaman.wallJumpVel = MegamanValues.WALL_JUMP_VEL

        body = Body(BodyType.DYNAMIC)
        body.putProperty(ConstKeys.BODY_OVER_GROUND, { overGround })
        megaman.addComponent(BodyComponent(body))
        megaman.addComponent(ControllerComponent().also { it.input = InputSource { frame } })
        megaman.addComponent(TeleportableComponent())
        megaman.addComponent(PostActionMomentumComponent(1f, 1f, { 1f }, { true }, { false }, { false }))
        behaviors = BehaviorsComponent()
        megaman.addComponent(behaviors)
        megaman.addRuleComponents()

        jump = megaman.jumpBehavior()
        behaviors.addBehavior(BehaviorType.JUMPING, jump)
    }

    afterEach { unmockkAll() }

    describe("the start") {
        it("starts on a new press of A over the ground") {
            press(ButtonStatus.JUST_PRESSED)
            jump.evaluate(0f) shouldBe true
        }

        it("does not start with A released") {
            jump.evaluate(0f) shouldBe false
        }

        it("does not start with A held from before") {
            press(ButtonStatus.PRESSED)
            jump.evaluate(0f) shouldBe false
        }

        it("does not start in the air") {
            overGround = false
            press(ButtonStatus.JUST_PRESSED)
            jump.evaluate(0f) shouldBe false
        }

        it("starts in the air from a wall slide") {
            overGround = false
            setBehaviorActive(BehaviorType.WALL_SLIDING)
            press(ButtonStatus.JUST_PRESSED)
            jump.evaluate(0f) shouldBe true
        }

        it("does not start when the A task is not jump") {
            megaman.aButtonTask = AButtonTask.AIR_DASH
            press(ButtonStatus.JUST_PRESSED)
            jump.evaluate(0f) shouldBe false
        }

        it("does not start with DOWN held over the ground") {
            press(ButtonStatus.JUST_PRESSED, MegaControllerButton.DOWN)
            jump.evaluate(0f) shouldBe false
        }

        it("does not start during a needle spin") {
            megaman.currentWeapon = MegamanWeapon.NEEDLE_SPIN
            megaman.shooting = true
            press(ButtonStatus.JUST_PRESSED)
            jump.evaluate(0f) shouldBe false
        }

        it("does not start with the head on a block") {
            body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
            press(ButtonStatus.JUST_PRESSED)
            jump.evaluate(0f) shouldBe false
        }
    }

    describe("the jump speed") {
        it("sets the up speed to the jump speed and keeps the side speed") {
            body.physics.velocity.set(3f, 0f)
            jump.init()
            body.physics.velocity.y shouldBe (MegamanValues.JUMP_VEL * ConstVals.PPM plusOrMinus TOLERANCE)
            body.physics.velocity.x shouldBe (3f plusOrMinus TOLERANCE)
        }

        it("sets the up friction to 1") {
            jump.init()
            body.physics.frictionOnSelf.y shouldBe 1f
        }

        it("scales the jump speed with the jump boost") {
            megaman.services.gameState.addEnhancement(MegaEnhancement.JUMP_BOOST)
            jump.init()
            body.physics.velocity.y shouldBe
                (MegamanValues.JUMP_VEL * MegaEnhancement.JUMP_BOOST_SCALAR * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("uses the wall jump speeds from a wall slide") {
            setBehaviorActive(BehaviorType.WALL_SLIDING)
            jump.init()
            body.physics.velocity.y shouldBe (MegamanValues.WALL_JUMP_VEL * ConstVals.PPM plusOrMinus TOLERANCE)
            body.physics.velocity.x shouldBe
                (MegamanValues.WALL_JUMP_HORIZONTAL * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("lets Megaman make a land sound") {
            megaman.canMakeLandSound = false
            jump.init()
            megaman.canMakeLandSound shouldBe true
        }
    }

    describe("the jump while active") {
        it("goes on while A is held and Megaman moves up") {
            press(ButtonStatus.JUST_PRESSED)
            jump.update(0f)
            press(ButtonStatus.PRESSED)
            body.physics.velocity.y = 1f
            jump.evaluate(0f) shouldBe true
        }

        it("stops on the release of A and sets the up speed to 0") {
            press(ButtonStatus.JUST_PRESSED)
            jump.update(0f)
            body.physics.velocity.y = 5f
            frame = InputFrame.RELEASED
            jump.update(0f)
            jump.isActive() shouldBe false
            body.physics.velocity.y shouldBe 0f
        }

        it("stops when Megaman falls") {
            press(ButtonStatus.JUST_PRESSED)
            jump.update(0f)
            press(ButtonStatus.PRESSED)
            body.physics.velocity.y = -1f
            jump.evaluate(0f) shouldBe false
        }
    }

    describe("the body direction") {
        it("jumps along x with the body turned left") {
            body.direction = Direction.LEFT
            megaman.jumpVel = -MegamanValues.JUMP_VEL
            jump.init()
            body.physics.velocity.x shouldBe (-MegamanValues.JUMP_VEL * ConstVals.PPM plusOrMinus TOLERANCE)
            body.physics.frictionOnSelf.x shouldBe 1f
        }
    }
})
