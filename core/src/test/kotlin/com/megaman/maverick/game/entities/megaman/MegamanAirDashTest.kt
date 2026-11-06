package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.entities.megaman.behaviors.airDashBehavior
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.unmockkAll
import io.mockk.verify

private const val TOLERANCE = 0.001f

class MegamanAirDashTest : DescribeSpec({

    lateinit var rig: MegamanMovesRig
    lateinit var dash: IBehavior

    fun pressA(status: ButtonStatus = ButtonStatus.JUST_PRESSED) = rig.press(MegaControllerButton.A to status)

    fun startDash() {
        pressA()
        dash.update(0f)
    }

    beforeEach {
        rig = MegamanMovesRig()
        dash = rig.megaman.airDashBehavior()
        rig.behaviors.addBehavior(BehaviorType.AIR_DASHING, dash)
    }

    afterEach { unmockkAll() }

    describe("the start") {
        it("starts on a new press of A in the air when the A task is air dash") {
            pressA()
            dash.evaluate(0f) shouldBe true
        }

        it("does not start when the A task is not air dash") {
            rig.megaman.aButtonTask = AButtonTask.JUMP
            pressA()
            dash.evaluate(0f) shouldBe false
        }

        it("does not start with A held from before") {
            pressA(ButtonStatus.PRESSED)
            dash.evaluate(0f) shouldBe false
        }

        it("starts on SELECT, not on A, when the select action is air dash") {
            rig.services.players.settings(PlayerSlot(0)).selectButtonAction = SelectButtonAction.AIR_DASH
            pressA()
            dash.evaluate(0f) shouldBe false
            rig.press(MegaControllerButton.SELECT to ButtonStatus.JUST_PRESSED)
            dash.evaluate(0f) shouldBe true
        }

        it("does not start over the ground, but does with the Rush Jet") {
            rig.overGround = true
            pressA()
            dash.evaluate(0f) shouldBe false
            rig.megaman.currentWeapon = MegamanWeapon.RUSH_JET
            dash.evaluate(0f) shouldBe true
        }

        it("does not start with the Rush Jet empty or with UP held") {
            rig.megaman.currentWeapon = MegamanWeapon.RUSH_JET
            every { rig.megaman.weaponsHandler.isDepleted(MegamanWeapon.RUSH_JET) } returns true
            pressA()
            dash.evaluate(0f) shouldBe false
            every { rig.megaman.weaponsHandler.isDepleted(MegamanWeapon.RUSH_JET) } returns false
            rig.press(MegaControllerButton.A to ButtonStatus.JUST_PRESSED, MegaControllerButton.UP to ButtonStatus.PRESSED)
            dash.evaluate(0f) shouldBe false
        }

        it("does not start during a wall slide or facing a block") {
            rig.setActive(BehaviorType.WALL_SLIDING)
            pressA()
            dash.evaluate(0f) shouldBe false
        }

        it("does not start facing a touched block") {
            rig.body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
            pressA()
            dash.evaluate(0f) shouldBe false
        }

        it("does not start during a needle spin") {
            rig.megaman.currentWeapon = MegamanWeapon.NEEDLE_SPIN
            rig.megaman.shooting = true
            pressA()
            dash.evaluate(0f) shouldBe false
        }
    }

    describe("the dash") {
        it("turns gravity off, uses the A task and marks the dash at the start") {
            startDash()
            rig.body.physics.gravityOn shouldBe false
            rig.megaman.aButtonTask shouldBe AButtonTask.JUMP
            rig.megaman.airDashConsumed shouldBe true
            rig.megaman.airDashDirection shouldBe Direction.UP
        }

        it("moves at the air dash speed in the facing direction") {
            startDash()
            pressA(ButtonStatus.PRESSED)
            dash.update(0.01f)
            rig.body.physics.velocity.x shouldBe (MegamanValues.AIR_DASH_VEL * ConstVals.PPM plusOrMinus TOLERANCE)
            rig.body.physics.velocity.y shouldBe 0f
        }

        it("moves at the water speed in water") {
            rig.body.setBodySense(BodySense.IN_WATER, true)
            startDash()
            pressA(ButtonStatus.PRESSED)
            dash.update(0.01f)
            rig.body.physics.velocity.x shouldBe
                (MegamanValues.WATER_AIR_DASH_VEL * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("moves faster with the Rush Jet") {
            rig.megaman.currentWeapon = MegamanWeapon.RUSH_JET
            startDash()
            pressA(ButtonStatus.PRESSED)
            dash.update(0.01f)
            rig.body.physics.velocity.x shouldBe (MegamanValues.AIR_DASH_VEL *
                MegamanValues.RUSH_JET_AIR_DASH_SCALAR * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("turns the speed with the facing") {
            startDash()
            pressA(ButtonStatus.PRESSED)
            dash.update(0.01f)
            rig.megaman.facing = Facing.LEFT
            dash.update(0.01f)
            rig.body.physics.velocity.x shouldBe (-MegamanValues.AIR_DASH_VEL * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("does not set the speed against a wall it faces") {
            startDash()
            pressA(ButtonStatus.PRESSED)
            rig.body.physics.velocity.x = 0f
            rig.body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
            dash.act(0.01f)
            rig.body.physics.velocity.x shouldBe 0f
        }

        it("goes on until the min time with A released") {
            startDash()
            rig.press()
            dash.update(MegamanValues.AIR_DASH_MIN_TIME / 2f)
            dash.isActive() shouldBe true
        }

        it("ends after the min time with A released") {
            startDash()
            rig.press()
            dash.update(MegamanValues.AIR_DASH_MIN_TIME + 0.01f)
            dash.update(0.01f)
            dash.isActive() shouldBe false
        }

        it("ends at the max time with A held") {
            startDash()
            pressA(ButtonStatus.PRESSED)
            var time = 0f
            while (time < MegamanValues.AIR_DASH_MAX_TIME + 0.05f) {
                dash.update(0.02f)
                time += 0.02f
            }
            dash.isActive() shouldBe false
        }
    }

    describe("the end") {
        it("turns gravity on and adds the end bump in the facing direction") {
            startDash()
            rig.body.physics.velocity.x = 0f
            dash.end()
            rig.body.physics.gravityOn shouldBe true
            rig.body.physics.velocity.x shouldBe
                (MegamanValues.AIR_DASH_END_BUMP * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("adds the water end bump in water") {
            rig.body.setBodySense(BodySense.IN_WATER, true)
            startDash()
            rig.body.physics.velocity.x = 0f
            dash.end()
            rig.body.physics.velocity.x shouldBe
                (MegamanValues.WATER_AIR_DASH_END_BUMP * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("resets the jetpack with the Rush Jet") {
            rig.megaman.currentWeapon = MegamanWeapon.RUSH_JET
            startDash()
            dash.end()
            verify { rig.megaman.jetpackHandler.reset() }
        }
    }
})
