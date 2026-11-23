package com.megaman.maverick.game.players.bots

import com.maverick.game.engine.entities.body
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.state.GameClock
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs

// TODO: add a BotBrain interface when a second brain exists (item 178d)
class FollowBrain(private val players: MegaPlayers, private val clock: GameClock) {

    companion object {
        // TODO: the delay counts render-frame time, so at a low frame rate the copy is coarser; a fixed tick comes
        //  with item 010
        const val DELAY = 16f / 60f
        const val RECORDS = 64
        const val STEER_DISTANCE = 1f
        const val ASSIST_HEIGHT = 2f
        const val STUCK_DISTANCE = 0.125f
        const val STUCK_TIME = 1f
        const val ASSIST_JUMP_TIME = 0.5f

        private val LEADER = PlayerSlot(0)
        private val BOT = PlayerSlot(1)

        private val BUTTONS = MegaControllerButton.entries.toTypedArray()
        private val START_BIT = bit(MegaControllerButton.START)
        private val LEFT_BIT = bit(MegaControllerButton.LEFT)
        private val RIGHT_BIT = bit(MegaControllerButton.RIGHT)
        private val A_BIT = bit(MegaControllerButton.A)

        private fun bit(button: MegaControllerButton) = 1L shl button.index
    }

    private val recordedHeld = LongArray(RECORDS)
    private val recordedX = FloatArray(RECORDS)
    private val recordedY = FloatArray(RECORDS)
    private val recordedDelta = FloatArray(RECORDS)
    private var newest = -1
    private var count = 0

    private var lastHeld = 0L
    private var stuckTime = 0f
    private var stuckX = 0f
    private var assisting = false
    private var assistTime = 0f

    fun held(leaderFrame: InputFrame, delta: Float): Long {
        if (clock.paused) return lastHeld

        val leader = players.get(LEADER)
        if (leader == null || !players.isLive(leader)) {
            count = 0
            return output(0L)
        }
        val leaderCenter = leader.body.getCenter()
        record(heldBits(leaderFrame), leaderCenter.x, leaderCenter.y, delta)

        val bot = players.get(BOT)
        if (bot == null || !players.isLive(bot)) {
            assisting = false
            stuckTime = 0f
            return output(0L)
        }

        val index = delayedIndex()
        if (index < 0) return output(0L)

        var held = recordedHeld[index] and START_BIT.inv()
        val botCenter = bot.body.getCenter()
        val botX = botCenter.x
        val botY = botCenter.y
        val targetX = recordedX[index]
        val targetY = recordedY[index]

        val dx = targetX - botX
        if (abs(dx) > STEER_DISTANCE * ConstVals.PPM)
            held = (held and (LEFT_BIT or RIGHT_BIT).inv()) or if (dx > 0f) RIGHT_BIT else LEFT_BIT

        if (!assisting) {
            val leaderAbove = targetY - botY >= ASSIST_HEIGHT * ConstVals.PPM
            val standing = bot.body.isSensing(BodySense.FEET_ON_GROUND)
            if (leaderAbove && standing && abs(botX - stuckX) <= STUCK_DISTANCE * ConstVals.PPM) {
                stuckTime += delta
                if (stuckTime >= STUCK_TIME) {
                    assisting = true
                    assistTime = 0f
                }
            } else {
                stuckTime = 0f
                stuckX = botX
            }
        }

        if (assisting) {
            held = if (assistTime == 0f) held and A_BIT.inv() else held or A_BIT
            assistTime += delta
            if (assistTime > ASSIST_JUMP_TIME) stopAssist(botX)
        }

        return output(held)
    }

    private fun output(held: Long): Long {
        lastHeld = held
        return held
    }

    private fun stopAssist(botX: Float) {
        assisting = false
        stuckTime = 0f
        stuckX = botX
    }

    private fun heldBits(frame: InputFrame): Long {
        var held = 0L
        for (button in BUTTONS) if (frame.isPressed(button)) held = held or bit(button)
        return held
    }

    private fun record(held: Long, x: Float, y: Float, delta: Float) {
        newest = (newest + 1) % RECORDS
        recordedHeld[newest] = held
        recordedX[newest] = x
        recordedY[newest] = y
        recordedDelta[newest] = delta
        if (count < RECORDS) count++
    }

    private fun delayedIndex(): Int {
        var age = 0f
        var index = newest
        for (step in 0 until count) {
            if (age >= DELAY) return index
            if (step == count - 1) break
            age += recordedDelta[index]
            index = (index - 1 + RECORDS) % RECORDS
        }
        return if (count == RECORDS) index else -1
    }
}
