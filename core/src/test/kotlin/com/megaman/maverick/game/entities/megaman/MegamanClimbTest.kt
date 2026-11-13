package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.behaviors.climbBehavior
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

class MegamanClimbTest : DescribeSpec({

    val ppm = ConstVals.PPM.toFloat()
    val climbSpeed = MegamanValues.CLIMB_VEL * ppm

    lateinit var rig: MegamanMovesRig
    lateinit var climb: IBehavior
    lateinit var ladderBody: Body

    fun place(x: Float, y: Float) {
        rig.body.setPosition(x * ppm, y * ppm)
    }

    fun touch(head: Boolean, feet: Boolean) {
        rig.body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, head)
        rig.body.setBodySense(BodySense.FEET_TOUCHING_LADDER, feet)
    }

    fun grabFromFloor() {
        place(4.2f, 0f)
        touch(head = true, feet = true)
        rig.press(MegaControllerButton.UP to ButtonStatus.PRESSED)
        climb.update(0f)
    }

    beforeEach {
        rig = MegamanMovesRig()
        rig.megaman.shooting = false
        rig.body.setSize(1f * ppm, 1.5f * ppm)
        val sensor = { GameRectangle().setSize(0.5f * ppm, 0.25f * ppm) }
        rig.body.addFixture(Fixture(rig.body, FixtureType.HEAD, sensor(), offsetFromBodyAttachment = Vector2(0f, 0.75f * ppm)))
        rig.body.addFixture(Fixture(rig.body, FixtureType.FEET, sensor(), offsetFromBodyAttachment = Vector2(0f, -0.75f * ppm)))

        ladderBody = Body(BodyType.ABSTRACT).also { it.set(4f * ppm, 0f, 1f * ppm, 6f * ppm) }
        val ladder = mockk<IBodyEntity>(relaxed = true)
        every { ladder.body } returns ladderBody
        rig.body.putProperty(ConstKeys.LADDER, ladder)

        climb = rig.megaman.climbBehavior()
        rig.behaviors.addBehavior(BehaviorType.CLIMBING, climb)
    }

    afterEach { unmockkAll() }

    describe("the grab") {
        it("grabs with UP when the head is on the ladder, and snaps to its center") {
            grabFromFloor()
            climb.isActive() shouldBe true
            (rig.body.getX() + rig.body.getWidth() / 2f) shouldBe (4.5f * ppm plusOrMinus 0.01f)
            rig.body.physics.gravityOn shouldBe false
        }

        it("grabs with DOWN when the feet are on the ladder top, and sets the body under the top") {
            place(4.2f, 6f)
            touch(head = false, feet = true)
            rig.press(MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            climb.update(0f)
            climb.isActive() shouldBe true
            rig.body.getMaxY() shouldBe (6f * ppm plusOrMinus 0.01f)
        }

        it("does not grab with no direction held") {
            place(4.2f, 0f)
            touch(head = true, feet = true)
            climb.evaluate(0f) shouldBe false
        }

        it("does not grab during a jump") {
            place(4.2f, 0f)
            touch(head = true, feet = true)
            rig.press(MegaControllerButton.UP to ButtonStatus.PRESSED)
            rig.setActive(BehaviorType.JUMPING)
            climb.evaluate(0f) shouldBe false
        }

        it("does not grab while Needle Spin shoots") {
            place(4.2f, 0f)
            touch(head = true, feet = true)
            rig.press(MegaControllerButton.UP to ButtonStatus.PRESSED)
            rig.megaman.currentWeapon = MegamanWeapon.NEEDLE_SPIN
            rig.megaman.shooting = true
            climb.evaluate(0f) shouldBe false
        }

        it("sets the A button task and stops the land sound") {
            rig.megaman.canMakeLandSound = true
            grabFromFloor()
            rig.megaman.aButtonTask shouldBe AButtonTask.AIR_DASH
            rig.megaman.canMakeLandSound shouldBe false
        }
    }

    describe("the climb") {
        it("moves up on UP, down on DOWN, and stops with no direction") {
            grabFromFloor()
            climb.update(0f)
            rig.body.physics.velocity.y shouldBe (climbSpeed plusOrMinus 0.01f)
            rig.press(MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            climb.update(0f)
            rig.body.physics.velocity.y shouldBe (-climbSpeed plusOrMinus 0.01f)
            rig.press()
            climb.update(0f)
            rig.body.physics.velocity.y shouldBe 0f
        }

        it("moves up when UP and DOWN are both held with the gravity UP") {
            grabFromFloor()
            rig.press(MegaControllerButton.UP to ButtonStatus.PRESSED, MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            climb.update(0f)
            rig.body.physics.velocity.y shouldBe (climbSpeed plusOrMinus 0.01f)
        }

        it("gives DOWN the priority when both are held with the gravity DOWN") {
            grabFromFloor()
            rig.body.direction = Direction.DOWN
            rig.press(MegaControllerButton.UP to ButtonStatus.PRESSED, MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            climb.update(0f)
            rig.body.physics.velocity.y shouldBe (climbSpeed plusOrMinus 0.01f)
        }

        it("climbs along x with the gravity RIGHT and LEFT") {
            grabFromFloor()
            rig.body.direction = Direction.RIGHT
            climb.update(0f)
            rig.body.physics.velocity.x shouldBe (climbSpeed plusOrMinus 0.01f)
            rig.body.direction = Direction.LEFT
            climb.update(0f)
            rig.body.physics.velocity.x shouldBe (-climbSpeed plusOrMinus 0.01f)
        }

        it("holds a positive zero speed with no direction in the gravity DOWN") {
            grabFromFloor()
            rig.body.direction = Direction.DOWN
            rig.press()
            climb.update(0f)
            (1f / rig.body.physics.velocity.y) shouldBe Float.POSITIVE_INFINITY
        }

        it("stops while shooting") {
            grabFromFloor()
            rig.megaman.shooting = true
            climb.update(0f)
            rig.body.physics.velocity.y shouldBe 0f
        }


        it("reverses the controls with the gravity DOWN") {
            grabFromFloor()
            rig.body.direction = Direction.DOWN
            rig.press(MegaControllerButton.DOWN to ButtonStatus.PRESSED)
            climb.update(0f)
            rig.body.physics.velocity.y shouldBe (climbSpeed plusOrMinus 0.01f)
        }
    }

    describe("the release") {
        it("lets go on A, with gravity on and no speed") {
            grabFromFloor()
            rig.press(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)
            climb.update(0f)
            climb.isActive() shouldBe false
            rig.body.physics.gravityOn shouldBe true
            rig.body.physics.velocity.y shouldBe 0f
        }

        it("lets go when the body center is half a tile over the ladder top") {
            grabFromFloor()
            place(4f, 5.8f)
            touch(head = false, feet = true)
            climb.update(0f)
            climb.isActive() shouldBe false
        }

        it("keeps the climb just under the ladder top") {
            grabFromFloor()
            place(4f, 5f)
            touch(head = false, feet = true)
            climb.update(0f)
            climb.isActive() shouldBe true
        }
    }
})
