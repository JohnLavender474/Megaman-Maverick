package com.megaman.maverick.game.players.bots

import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputFrameBuilder
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.TestHealthPlayer
import com.megaman.maverick.game.state.GameClock
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

private const val STEP = FollowBrain.DELAY / 2f

private fun bit(button: MegaControllerButton) = 1L shl button.index

private class BrainRig {
    val players = MegaPlayers(2)
    val clock = GameClock()
    val services = testServices(players = players, clock = clock)
    val leader = player(0, 100f, 20f)
    val bot = player(1, 100f, 20f)
    val brain = FollowBrain(players, clock)
    private val leaderFrames = InputFrameBuilder(MegaControllerButton.entries)

    private fun player(slot: Int, x: Float, y: Float): TestHealthPlayer {
        val player = TestHealthPlayer(services)
        player.body.setCenter(x, y)
        player.body.setBodySense(BodySense.FEET_ON_GROUND, true)
        players.register(PlayerSlot(slot), player)
        player.existenceState = EntityExistenceState.LIVE
        return player
    }

    fun frame(held: Long): InputFrame = leaderFrames.build(held)

    fun step(held: Long, delta: Float = STEP) = brain.held(frame(held), delta)
}

class FollowBrainTest : DescribeSpec({

    val b = bit(MegaControllerButton.B)
    val a = bit(MegaControllerButton.A)
    val left = bit(MegaControllerButton.LEFT)
    val right = bit(MegaControllerButton.RIGHT)
    val start = bit(MegaControllerButton.START)
    val select = bit(MegaControllerButton.SELECT)

    describe("the delay") {
        it("gives the leader's held bits from the delay back, and nothing before that") {
            val rig = BrainRig()
            rig.step(b) shouldBe 0L
            rig.step(0L) shouldBe 0L
            rig.step(0L) shouldBe b
            rig.step(0L) shouldBe 0L
        }
    }

    describe("the START mask") {
        it("never copies START") {
            val rig = BrainRig()
            rig.step(start or b)
            rig.step(start or b)
            rig.step(start or b) shouldBe b
        }
    }

    describe("the steer") {
        it("presses toward the recorded leader in place of the copied side when the bot is far on X") {
            val rig = BrainRig()
            rig.leader.body.setCenter(100f + 3f * ConstVals.PPM, 20f)
            repeat(2) { rig.step(left) }
            rig.step(left) shouldBe right
        }

        it("keeps the copied side when the bot is near on X") {
            val rig = BrainRig()
            rig.leader.body.setCenter(100f + 0.5f * ConstVals.PPM, 20f)
            repeat(2) { rig.step(left) }
            rig.step(left) shouldBe left
        }
    }

    describe("the jump assist") {
        fun aboveRig(): BrainRig {
            val rig = BrainRig()
            rig.leader.body.setCenter(100f, 20f + 3f * ConstVals.PPM)
            return rig
        }

        it("releases A and then holds A for the assist time after the bot stands still under the leader") {
            val rig = aboveRig()
            val delta = 0.1f
            val outputs = (0 until 30).map { rig.step(0L, delta) and a }
            val firstHeld = outputs.indexOfFirst { it != 0L }
            (firstHeld * delta >= FollowBrain.STUCK_TIME) shouldBe true
            val heldFrames = outputs.drop(firstHeld).takeWhile { it != 0L }.size
            (heldFrames * delta) shouldBe (FollowBrain.ASSIST_JUMP_TIME plusOrMinus delta)
        }

        it("holds A when the leader is exactly the assist height above") {
            val rig = BrainRig()
            rig.leader.body.setCenter(100f, 20f + FollowBrain.ASSIST_HEIGHT * ConstVals.PPM)
            (0 until 30).map { rig.step(0L, 0.1f) and a }.any { it != 0L } shouldBe true
        }

        it("gives no A while the bot moves on X") {
            val rig = aboveRig()
            val outputs = (0 until 30).map {
                rig.bot.body.setCenter(100f + it * 0.5f * ConstVals.PPM, 20f)
                rig.step(0L, 0.1f) and a
            }
            outputs.all { it == 0L } shouldBe true
        }

        it("gives no A while the bot is in the air") {
            val rig = aboveRig()
            rig.bot.body.setBodySense(BodySense.FEET_ON_GROUND, false)
            (0 until 30).map { rig.step(0L, 0.1f) and a }.all { it == 0L } shouldBe true
        }
    }

    describe("the pause") {
        it("records nothing and keeps the last output while the game is paused") {
            val rig = BrainRig()
            rig.step(b)
            rig.step(0L)
            rig.clock.paused = true
            repeat(10) { rig.step(select) shouldBe 0L }
            rig.clock.paused = false
            rig.step(0L) shouldBe b
        }
    }

    describe("a player that is not live") {
        it("gives nothing while the bot is not live") {
            val rig = BrainRig()
            rig.bot.existenceState = EntityExistenceState.UNSPAWNED
            repeat(4) { rig.step(b) shouldBe 0L }
        }

        it("gives nothing while the leader is not live, and then starts a new record") {
            val rig = BrainRig()
            repeat(3) { rig.step(b) }
            rig.leader.existenceState = EntityExistenceState.UNSPAWNED
            rig.step(b) shouldBe 0L
            rig.leader.existenceState = EntityExistenceState.LIVE
            rig.step(0L) shouldBe 0L
            rig.step(0L) shouldBe 0L
        }
    }
})
