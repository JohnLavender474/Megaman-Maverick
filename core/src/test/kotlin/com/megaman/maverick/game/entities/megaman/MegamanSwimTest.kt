package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.locks.unlock
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.players.AButtonTask
import com.megaman.maverick.game.players.aButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.unmockkAll

private const val TOLERANCE = 0.01f

class MegamanSwimTest : DescribeSpec({

    val swimSpeed = MegamanValues.SWIM_VEL_Y * ConstVals.PPM

    lateinit var rig: MegamanSpawnedRig

    fun swimming() = rig.active(BehaviorType.SWIMMING)

    fun stroke() = rig.step(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)

    fun stepFor(seconds: Float) {
        var time = 0f
        while (time < seconds - TOLERANCE) {
            rig.step()
            time += SPAWNED_RIG_DELTA
        }
    }

    fun turn(direction: Direction) {
        rig.megaman.direction = direction
        rig.megaman.unlock(MegaLockGroup.MOVEMENT, MegaLockReason.CAMERA_ROTATION)
    }

    beforeEach {
        rig = MegamanSpawnedRig()
        rig.ground(false)
        rig.water(true)
        rig.megaman.aButtonTask = AButtonTask.SWIM
        rig.body.physics.velocity.setZero()
    }

    afterEach {
        unmockkAll()
        clearAllMocks(answers = false, recordedCalls = true, childMocks = false)
    }

    describe("the start") {
        it("starts in water on a new press of A when the A task is swim") {
            stroke()
            swimming() shouldBe true
        }

        it("does not start with A held from before") {
            rig.step(MegaControllerButton.A to ButtonStatus.PRESSED)
            swimming() shouldBe false
        }

        it("does not start when the A task is jump or air dash") {
            rig.megaman.aButtonTask = AButtonTask.JUMP
            stroke()
            swimming() shouldBe false
            rig.megaman.aButtonTask = AButtonTask.AIR_DASH
            stroke()
            swimming() shouldBe false
        }

        it("does not start out of water") {
            rig.water(false)
            stroke()
            swimming() shouldBe false
        }

        it("does not start with the head on a block") {
            rig.body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
            stroke()
            swimming() shouldBe false
        }

        it("does not start while damaged") {
            rig.hurt()
            stroke()
            swimming() shouldBe false
        }

        it("does not start while Needle Spin shoots, and starts with Needle Spin when it does not shoot") {
            rig.megaman.currentWeapon = MegamanWeapons.NEEDLE_SPIN
            rig.megaman.shooting = true
            stroke()
            swimming() shouldBe false
            rig.megaman.shooting = false
            stroke()
            swimming() shouldBe true
        }

        it("does not start with the movement locked") {
            rig.megaman.lock(MegaLockGroup.MOVEMENT, MegaLockReason.CAMERA_ROTATION)
            stroke()
            swimming() shouldBe false
        }

        it("does not start out of play") {
            rig.megaman.getComponent(PlayPhaseComponent::class)!!.phase = PlayPhase.PRE_PLAY
            stroke()
            swimming() shouldBe false
        }

        it("does not start when he is destroyed") {
            rig.megaman.existenceState = EntityExistenceState.DESTROY_QUEUED
            stroke()
            swimming() shouldBe false
        }
    }

    describe("the stroke") {
        it("adds the swim speed up to the velocity he has") {
            rig.body.physics.velocity.set(3f, 2f)
            stroke()
            rig.body.physics.velocity.x shouldBe (3f plusOrMinus TOLERANCE)
            rig.body.physics.velocity.y shouldBe (2f + swimSpeed plusOrMinus TOLERANCE)
        }

        it("is 1.25 times stronger with the jump boost") {
            rig.services.gameState.addEnhancement(MegaEnhancement.JUMP_BOOST)
            stroke()
            rig.body.physics.velocity.y shouldBe
                (swimSpeed * MegaEnhancement.JUMP_BOOST_SCALAR plusOrMinus TOLERANCE)
        }

        it("pushes along the axis of each gravity direction, with the old signs (bug candidate in the 060m TODO.md)") {
            val expected = mapOf(
                Direction.UP to (0f to swimSpeed),
                Direction.DOWN to (0f to swimSpeed),
                Direction.LEFT to (-swimSpeed to 0f),
                Direction.RIGHT to (-swimSpeed to 0f)
            )
            expected.forEach { (direction, speed) ->
                turn(direction)
                stepFor(MegamanValues.SWIM_TIMER + 2f * SPAWNED_RIG_DELTA)
                swimming() shouldBe false
                rig.body.physics.velocity.setZero()
                stroke()
                swimming() shouldBe true
                rig.body.physics.velocity.x shouldBe (speed.first plusOrMinus TOLERANCE)
                rig.body.physics.velocity.y shouldBe (speed.second plusOrMinus TOLERANCE)
            }
        }
    }

    describe("the time") {
        it("goes on with A released until the swim time ends") {
            stroke()
            stepFor(MegamanValues.SWIM_TIMER - 2f * SPAWNED_RIG_DELTA)
            swimming() shouldBe true
            stepFor(3f * SPAWNED_RIG_DELTA)
            swimming() shouldBe false
        }

        it("ends when he leaves the water") {
            stroke()
            rig.water(false)
            rig.step()
            swimming() shouldBe false
        }

        it("ends when the head touches a block") {
            stroke()
            rig.body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
            rig.step()
            swimming() shouldBe false
        }

        it("ends on damage") {
            stroke()
            rig.hurt()
            rig.step()
            swimming() shouldBe false
        }

        it("gives the full swim time to a new stroke after an early end") {
            stroke()
            stepFor(2f * SPAWNED_RIG_DELTA)
            rig.water(false)
            rig.step()
            swimming() shouldBe false
            rig.water(true)
            stroke()
            stepFor(MegamanValues.SWIM_TIMER - 2f * SPAWNED_RIG_DELTA)
            swimming() shouldBe true
        }

        it("gives the full swim time to the next stroke after a stroke that ran to its end") {
            stroke()
            stepFor(MegamanValues.SWIM_TIMER + 2f * SPAWNED_RIG_DELTA)
            swimming() shouldBe false
            stroke()
            stepFor(MegamanValues.SWIM_TIMER - 2f * SPAWNED_RIG_DELTA)
            swimming() shouldBe true
        }
    }
})
