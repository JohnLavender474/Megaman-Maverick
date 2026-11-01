package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.behaviors.crouchBehavior
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.math.max

private const val TINY_DELTA = 0.0001f

class MegamanCrouchDelayTest : DescribeSpec({

    val longDelta = max(MegamanValues.BUTTON_TO_CROUCH_DELAY, MegamanValues.GROUND_SLIDE_TO_CROUCH_DELAY) + 0.01f

    lateinit var crouch: IBehavior
    var groundSliding = false
    var frame = InputFrame.RELEASED

    fun holdDown() {
        frame = InputFrame.RELEASED.with(MegaControllerButton.DOWN, ButtonStatus.PRESSED)
    }

    fun releaseDown() {
        frame = InputFrame.RELEASED
    }

    beforeEach {
        groundSliding = false
        frame = InputFrame.RELEASED

        val megaman = Megaman(testServices(engine = mockk<GameEngine>(relaxed = true)))
        megaman.weaponsHandler = mockk(relaxed = true)
        megaman.currentWeapon = MegamanWeapon.MEGA_BUSTER
        megaman.existenceState = EntityExistenceState.LIVE

        val body = Body(BodyType.DYNAMIC)
        body.setBodySense(BodySense.FEET_ON_GROUND, true)
        megaman.addComponent(BodyComponent(body))
        megaman.addComponent(ControllerComponent().also { it.input = InputSource { frame } })

        val behaviors = BehaviorsComponent()
        val groundSlide = mockk<IBehavior>(relaxed = true)
        every { groundSlide.isActive() } answers { groundSliding }
        behaviors.addBehavior(BehaviorType.GROUND_SLIDING, groundSlide)
        megaman.addComponent(behaviors)

        crouch = megaman.crouchBehavior()
    }

    afterEach { unmockkAll() }

    describe("the crouch delays") {
        it("starts no crouch before the delays end") {
            holdDown()

            crouch.evaluate(TINY_DELTA) shouldBe false
        }

        it("starts a crouch after both delays end") {
            holdDown()

            crouch.evaluate(longDelta) shouldBe true
        }

        it("adds up the delta across frames") {
            holdDown()

            crouch.evaluate(longDelta / 2f) shouldBe false
            crouch.evaluate(longDelta / 2f) shouldBe true
        }

        it("restarts the button delay when DOWN is released") {
            holdDown()
            crouch.evaluate(longDelta) shouldBe true

            releaseDown()
            crouch.evaluate(TINY_DELTA) shouldBe false

            holdDown()
            crouch.evaluate(TINY_DELTA) shouldBe false
            crouch.evaluate(longDelta) shouldBe true
        }

        it("restarts the ground slide delay during a ground slide") {
            holdDown()
            crouch.evaluate(longDelta) shouldBe true

            groundSliding = true
            crouch.evaluate(TINY_DELTA) shouldBe false

            groundSliding = false
            crouch.evaluate(TINY_DELTA) shouldBe false
            crouch.evaluate(longDelta) shouldBe true
        }

        it("stays ready while DOWN stays held") {
            holdDown()
            crouch.evaluate(longDelta) shouldBe true
            crouch.evaluate(TINY_DELTA) shouldBe true
        }
    }
})
