package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.behaviors.wallSlideBehavior
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll

class MegamanWallSlideTest : DescribeSpec({

    lateinit var rig: MegamanMovesRig
    lateinit var slide: IBehavior

    fun pressIntoLeftWall() {
        rig.body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
        rig.megaman.getComponent(RunComponent::class)!!.lastPressedSide = Facing.LEFT
        rig.press(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
    }

    beforeEach {
        rig = MegamanMovesRig()
        slide = rig.megaman.wallSlideBehavior()
        rig.behaviors.addBehavior(BehaviorType.WALL_SLIDING, slide)
    }

    afterEach { unmockkAll() }

    describe("the start") {
        it("starts in the air while the side into a touched wall is held") {
            pressIntoLeftWall()
            slide.evaluate(0f) shouldBe true
        }

        it("starts on the right wall too") {
            rig.body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
            rig.megaman.getComponent(RunComponent::class)!!.lastPressedSide = Facing.RIGHT
            rig.press(MegaControllerButton.RIGHT to ButtonStatus.PRESSED)
            slide.evaluate(0f) shouldBe true
        }

        it("does not start with no side held") {
            rig.body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            slide.evaluate(0f) shouldBe false
        }

        it("does not start with the side held away from the wall") {
            rig.body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            rig.megaman.getComponent(RunComponent::class)!!.lastPressedSide = Facing.RIGHT
            rig.press(MegaControllerButton.RIGHT to ButtonStatus.PRESSED)
            slide.evaluate(0f) shouldBe false
        }

        it("does not start with the feet on the ground") {
            pressIntoLeftWall()
            rig.body.setBodySense(BodySense.FEET_ON_GROUND, true)
            slide.evaluate(0f) shouldBe false
        }

        it("does not start during a jump") {
            pressIntoLeftWall()
            rig.setActive(BehaviorType.JUMPING)
            slide.evaluate(0f) shouldBe false
        }

        it("does not start while climbing") {
            pressIntoLeftWall()
            rig.setActive(BehaviorType.CLIMBING)
            slide.evaluate(0f) shouldBe false
        }

        it("does not start with the jetpack") {
            pressIntoLeftWall()
            rig.setActive(BehaviorType.JETPACKING)
            slide.evaluate(0f) shouldBe false
        }

        it("does not start with the feet on sand") {
            pressIntoLeftWall()
            rig.body.setBodySense(BodySense.FEET_ON_SAND, true)
            slide.evaluate(0f) shouldBe false
        }

        it("does not start while the not-allowed timer or the wall jump timer runs") {
            pressIntoLeftWall()
            rig.megaman.wallSlideNotAllowedTimer.reset()
            slide.evaluate(0f) shouldBe false
            rig.megaman.wallSlideNotAllowedTimer.setToEnd()
            rig.megaman.wallJumpTimer.reset()
            slide.evaluate(0f) shouldBe false
        }

        it("does not start during a needle spin") {
            pressIntoLeftWall()
            rig.megaman.currentWeapon = MegamanWeapons.NEEDLE_SPIN
            rig.megaman.shooting = true
            slide.evaluate(0f) shouldBe false
        }
    }

    describe("the slide") {
        it("faces away from the wall and sets the A task to jump at the start") {
            pressIntoLeftWall()
            slide.init()
            rig.megaman.facing shouldBe Facing.RIGHT
            rig.megaman.aButtonTask shouldBe AButtonTask.JUMP
        }

        it("applies the wall slide friction") {
            slide.act(0.1f)
            rig.body.physics.frictionOnSelf.y shouldBe
                (MegamanValues.WALL_SLIDE_FRICTION_TO_APPLY * ConstVals.PPM plusOrMinus 0.001f)
        }

        it("holds still with the rodent claws") {
            rig.megaman.currentWeapon = MegamanWeapons.RODENT_CLAWS
            rig.body.physics.velocity.y = -3f
            slide.act(0.1f)
            rig.body.physics.velocity.y shouldBe 0f
            rig.body.physics.gravityOn shouldBe false
        }

        it("gives a new air dash and turns gravity on at the end") {
            rig.body.physics.gravityOn = false
            rig.megaman.aButtonTask = AButtonTask.JUMP
            slide.end()
            rig.megaman.aButtonTask shouldBe AButtonTask.AIR_DASH
            rig.body.physics.gravityOn shouldBe true
        }

        it("keeps the A task at the end in water") {
            rig.body.setBodySense(BodySense.IN_WATER, true)
            rig.megaman.aButtonTask = AButtonTask.SWIM
            slide.end()
            rig.megaman.aButtonTask shouldBe AButtonTask.SWIM
        }
    }
})
