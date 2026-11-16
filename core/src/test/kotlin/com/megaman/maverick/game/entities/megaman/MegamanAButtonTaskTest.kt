package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.players.AButtonTask
import com.megaman.maverick.game.players.aButtonTask
import com.megaman.maverick.game.entities.state.TeleportableComponent
import com.megaman.maverick.game.players.onGravityDeviceSwitch
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.clearAllMocks
import io.mockk.unmockkAll

class MegamanAButtonTaskTest : DescribeSpec({

    val ppm = ConstVals.PPM.toFloat()

    lateinit var rig: MegamanSpawnedRig

    fun task() = rig.megaman.aButtonTask

    fun pressA() = rig.step(MegaControllerButton.A to ButtonStatus.JUST_PRESSED)

    fun holdA() = rig.step(MegaControllerButton.A to ButtonStatus.PRESSED)

    fun jumpIntoTheAir() {
        pressA()
        rig.active(BehaviorType.JUMPING) shouldBe true
        rig.ground(false)
        holdA()
    }

    fun endJumpInTheAir() {
        rig.step()
        rig.active(BehaviorType.JUMPING) shouldBe false
    }

    fun airDashAndEnd() {
        pressA()
        rig.active(BehaviorType.AIR_DASHING) shouldBe true
        var steps = 0
        while (rig.active(BehaviorType.AIR_DASHING) && steps < 40) {
            rig.step()
            steps++
        }
        rig.active(BehaviorType.AIR_DASHING) shouldBe false
    }

    fun land() {
        rig.ground(true)
        rig.feetBlock(ProcessState.BEGIN)
        rig.step()
    }

    fun putOnLadder() {
        val ladderBody = Body(BodyType.ABSTRACT).also { it.set(-0.5f * ppm, -3f * ppm, 2f * ppm, 8f * ppm) }
        val ladder = mockk<IBodyEntity>(relaxed = true)
        every { ladder.body } returns ladderBody
        rig.body.putProperty(ConstKeys.LADDER, ladder)
        rig.body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)
        rig.body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)
    }

    fun grabLadder() {
        rig.ground(false)
        putOnLadder()
        rig.step(MegaControllerButton.UP to ButtonStatus.PRESSED)
        rig.active(BehaviorType.CLIMBING) shouldBe true
    }

    beforeEach { rig = MegamanSpawnedRig() }

    afterEach {
        unmockkAll()
        clearAllMocks(answers = false, recordedCalls = true, childMocks = false)
    }

    describe("at the spawn") {
        it("is jump after the spawn, also when it was another task before") {
            rig.megaman.aButtonTask = AButtonTask.SWIM
            rig.spawn()
            task() shouldBe AButtonTask.JUMP
        }

        it("gives the air dash back at a spawn after a used dash") {
            jumpIntoTheAir()
            endJumpInTheAir()
            airDashAndEnd()
            task() shouldBe AButtonTask.JUMP
            rig.spawn()
            rig.ground(false)
            rig.step()
            task() shouldBe AButtonTask.AIR_DASH
        }
    }

    describe("on the ground") {
        it("jumps on A") {
            task() shouldBe AButtonTask.JUMP
            pressA()
            rig.active(BehaviorType.JUMPING) shouldBe true
            rig.active(BehaviorType.AIR_DASHING) shouldBe false
        }

        it("turns an air dash task into jump at the next step") {
            rig.megaman.aButtonTask = AButtonTask.AIR_DASH
            rig.step()
            task() shouldBe AButtonTask.JUMP
        }

        it("keeps an air dash task in water, on a ladder and in an air dash") {
            rig.megaman.aButtonTask = AButtonTask.AIR_DASH
            rig.water(true)
            rig.step()
            task() shouldBe AButtonTask.AIR_DASH
            rig.water(false)

            rig.setActive(BehaviorType.CLIMBING)
            rig.step()
            task() shouldBe AButtonTask.AIR_DASH

            rig.behaviors.addBehavior(BehaviorType.CLIMBING, mockk(relaxed = true))
            rig.setActive(BehaviorType.AIR_DASHING)
            rig.step()
            task() shouldBe AButtonTask.AIR_DASH

            rig.behaviors.addBehavior(BehaviorType.AIR_DASHING, mockk(relaxed = true))
            rig.step()
            task() shouldBe AButtonTask.JUMP
        }

        it("keeps a swim task") {
            rig.megaman.aButtonTask = AButtonTask.SWIM
            rig.step()
            task() shouldBe AButtonTask.SWIM
        }

        it("does not jump on A with a swim task") {
            rig.megaman.aButtonTask = AButtonTask.SWIM
            pressA()
            rig.active(BehaviorType.JUMPING) shouldBe false
        }
    }

    describe("in the air after a jump") {
        it("stays jump while the jump goes on") {
            jumpIntoTheAir()
            rig.active(BehaviorType.JUMPING) shouldBe true
            task() shouldBe AButtonTask.JUMP
        }

        it("becomes air dash when the jump ends, and A then air dashes") {
            jumpIntoTheAir()
            endJumpInTheAir()
            task() shouldBe AButtonTask.AIR_DASH
            pressA()
            rig.active(BehaviorType.AIR_DASHING) shouldBe true
            rig.active(BehaviorType.JUMPING) shouldBe false
        }

        it("becomes swim when the jump ends in water") {
            jumpIntoTheAir()
            rig.water(true)
            endJumpInTheAir()
            task() shouldBe AButtonTask.SWIM
        }

        it("stays jump in the air during a wall slide or a climb") {
            rig.ground(false)
            rig.setActive(BehaviorType.WALL_SLIDING)
            rig.step()
            task() shouldBe AButtonTask.JUMP

            rig.behaviors.addBehavior(BehaviorType.WALL_SLIDING, mockk(relaxed = true))
            rig.setActive(BehaviorType.CLIMBING)
            rig.step()
            task() shouldBe AButtonTask.JUMP
        }

        it("does not turn an air dash task into swim in water") {
            rig.ground(false)
            rig.megaman.aButtonTask = AButtonTask.AIR_DASH
            rig.water(true)
            rig.step()
            task() shouldBe AButtonTask.AIR_DASH
        }
    }

    describe("after an air dash") {
        it("is jump with the dash used, and A does nothing in the air") {
            jumpIntoTheAir()
            endJumpInTheAir()
            airDashAndEnd()
            task() shouldBe AButtonTask.JUMP
            pressA()
            rig.active(BehaviorType.AIR_DASHING) shouldBe false
            rig.active(BehaviorType.JUMPING) shouldBe false
            task() shouldBe AButtonTask.JUMP
        }

        it("gives the dash back after a landing") {
            jumpIntoTheAir()
            endJumpInTheAir()
            airDashAndEnd()
            land()
            task() shouldBe AButtonTask.JUMP
            jumpIntoTheAir()
            endJumpInTheAir()
            task() shouldBe AButtonTask.AIR_DASH
        }

        it("keeps the dash used after a fall with no landing") {
            jumpIntoTheAir()
            endJumpInTheAir()
            airDashAndEnd()
            rig.feetBlock(ProcessState.END)
            task() shouldBe AButtonTask.AIR_DASH
            rig.megaman.aButtonTask = AButtonTask.JUMP
            rig.step()
            task() shouldBe AButtonTask.JUMP
        }
    }

    describe("the feet") {
        it("sets jump when the feet touch a block, and again while they stay on it") {
            rig.ground(false)
            rig.megaman.aButtonTask = AButtonTask.AIR_DASH
            rig.feetBlock(ProcessState.BEGIN)
            task() shouldBe AButtonTask.JUMP
            rig.megaman.aButtonTask = AButtonTask.SWIM
            rig.feetBlock(ProcessState.CONTINUE)
            task() shouldBe AButtonTask.JUMP
        }

        it("sets air dash, or swim in water, when the feet leave a block with no ground below") {
            rig.ground(false)
            rig.feetBlock(ProcessState.END)
            task() shouldBe AButtonTask.AIR_DASH
            rig.water(true)
            rig.megaman.aButtonTask = AButtonTask.JUMP
            rig.feetBlock(ProcessState.END)
            task() shouldBe AButtonTask.SWIM
        }

        it("keeps the task when the feet leave a block over the ground") {
            rig.feetBlock(ProcessState.END)
            task() shouldBe AButtonTask.JUMP
        }

        it("sets air dash on a feet bounce out of water, and keeps the task in water") {
            rig.feetBounce()
            task() shouldBe AButtonTask.AIR_DASH
            rig.megaman.aButtonTask = AButtonTask.JUMP
            rig.water(true)
            rig.feetBounce()
            task() shouldBe AButtonTask.JUMP
        }
    }

    describe("in water") {
        it("sets swim when he enters or stays in water off the ground") {
            rig.ground(false)
            rig.waterContact(ProcessState.BEGIN)
            task() shouldBe AButtonTask.SWIM
            rig.megaman.aButtonTask = AButtonTask.JUMP
            rig.waterContact(ProcessState.CONTINUE)
            task() shouldBe AButtonTask.SWIM
        }

        it("keeps the task in water with the feet on the ground") {
            rig.waterContact(ProcessState.BEGIN)
            task() shouldBe AButtonTask.JUMP
            rig.waterContact(ProcessState.CONTINUE)
            task() shouldBe AButtonTask.JUMP
        }

        it("keeps the task in water during a wall slide") {
            rig.ground(false)
            rig.setActive(BehaviorType.WALL_SLIDING)
            rig.waterContact(ProcessState.BEGIN)
            task() shouldBe AButtonTask.JUMP
        }

        it("sets air dash when he leaves the water, also on the ground") {
            rig.megaman.aButtonTask = AButtonTask.SWIM
            rig.waterContact(ProcessState.END)
            task() shouldBe AButtonTask.AIR_DASH
        }

        it("swims on A in the air in water") {
            rig.ground(false)
            rig.water(true)
            rig.step()
            task() shouldBe AButtonTask.SWIM
            pressA()
            rig.active(BehaviorType.SWIMMING) shouldBe true
            rig.active(BehaviorType.AIR_DASHING) shouldBe false
            rig.active(BehaviorType.JUMPING) shouldBe false
        }
    }

    describe("the wall slide") {
        it("sets jump at the start and at each step of the slide") {
            val slide = rig.behavior(BehaviorType.WALL_SLIDING)
            rig.megaman.aButtonTask = AButtonTask.AIR_DASH
            rig.body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
            rig.press(MegaControllerButton.RIGHT to ButtonStatus.PRESSED)
            slide.init()
            task() shouldBe AButtonTask.JUMP
            rig.megaman.aButtonTask = AButtonTask.AIR_DASH
            slide.act(SPAWNED_RIG_DELTA)
            task() shouldBe AButtonTask.JUMP
        }

        it("sets air dash at the end out of water, and keeps the task in water") {
            val slide = rig.behavior(BehaviorType.WALL_SLIDING)
            slide.end()
            task() shouldBe AButtonTask.AIR_DASH
            rig.megaman.aButtonTask = AButtonTask.JUMP
            rig.water(true)
            slide.end()
            task() shouldBe AButtonTask.JUMP
        }
    }

    describe("the ladder") {
        it("sets air dash at the grab, and again when he leaves the ladder with A") {
            grabLadder()
            task() shouldBe AButtonTask.AIR_DASH
            rig.megaman.aButtonTask = AButtonTask.JUMP
            pressA()
            rig.active(BehaviorType.CLIMBING) shouldBe false
            task() shouldBe AButtonTask.AIR_DASH
        }

        it("sets air dash when he leaves the ladder with A, also with the dash used") {
            jumpIntoTheAir()
            endJumpInTheAir()
            airDashAndEnd()
            grabLadder()
            rig.megaman.aButtonTask = AButtonTask.JUMP
            pressA()
            rig.active(BehaviorType.CLIMBING) shouldBe false
            task() shouldBe AButtonTask.AIR_DASH
        }

        it("sets swim at the grab and at the end in water") {
            rig.water(true)
            grabLadder()
            task() shouldBe AButtonTask.SWIM
            rig.megaman.aButtonTask = AButtonTask.JUMP
            pressA()
            rig.active(BehaviorType.CLIMBING) shouldBe false
            task() shouldBe AButtonTask.SWIM
        }
    }

    describe("the gravity device and the teleport") {
        it("sets jump on a gravity switch with the feet on the ground, else air dash") {
            rig.megaman.aButtonTask = AButtonTask.SWIM
            rig.megaman.onGravityDeviceSwitch()
            task() shouldBe AButtonTask.JUMP
            rig.ground(false)
            rig.megaman.onGravityDeviceSwitch()
            task() shouldBe AButtonTask.AIR_DASH
        }

        it("sets air dash at the end of a teleport") {
            rig.megaman.require(TeleportableComponent::class).onEnd!!.invoke()
            task() shouldBe AButtonTask.AIR_DASH
        }
    }
})
