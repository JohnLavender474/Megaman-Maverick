package com.megaman.maverick.game.entities.bosses.elecdevil

import com.maverick.game.engine.common.random.GameRandom
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_BODY_WIDTH
import kotlin.math.max
import kotlin.math.min

enum class LiveWireBand { JUMP_OVER, CROUCH_UNDER }

object LiveWireLinks {

    const val CROUCH_UNDER_MIN_ROW = 2

    const val JUMP_CLEAR_TIME = 0.5f
    const val JUMP_TIMING_SLACK = 0.15f
    const val PIECE_RADIUS = 0.5f

    fun bandOf(row: Int) = if (row < CROUCH_UNDER_MIN_ROW) LiveWireBand.JUMP_OVER else LiveWireBand.CROUCH_UNDER

    fun jumpOverPassGap(speed: Float) =
        max(0f, JUMP_CLEAR_TIME - JUMP_TIMING_SLACK - (2f * PIECE_RADIUS + MEGAMAN_BODY_WIDTH) / speed)

    fun choose(
        pieces: Int,
        links: Int,
        minGap: Int,
        startRows: IntArray,
        random: GameRandom,
        candidates: IntArray,
        linkedToPrevious: BooleanArray
    ): BooleanArray {
        linkedToPrevious.fill(false)
        repeat(links) {
            val firstBand = if (random.nextBoolean()) LiveWireBand.JUMP_OVER else LiveWireBand.CROUCH_UNDER
            var count = collectCandidates(firstBand, pieces, minGap, startRows, candidates, linkedToPrevious)
            if (count == 0) {
                val otherBand = when (firstBand) {
                    LiveWireBand.JUMP_OVER -> LiveWireBand.CROUCH_UNDER
                    LiveWireBand.CROUCH_UNDER -> LiveWireBand.JUMP_OVER
                }
                count = collectCandidates(otherBand, pieces, minGap, startRows, candidates, linkedToPrevious)
            }
            if (count == 0) return linkedToPrevious
            linkedToPrevious[candidates[random.nextInt(0, count - 1)]] = true
        }
        return linkedToPrevious
    }

    private fun collectCandidates(
        band: LiveWireBand,
        pieces: Int,
        minGap: Int,
        startRows: IntArray,
        candidates: IntArray,
        linkedToPrevious: BooleanArray
    ): Int {
        var count = 0
        for (index in 1 until pieces) {
            val sameBand = bandOf(startRows[index - 1]) == band && bandOf(startRows[index]) == band
            if (sameBand && canLink(index, pieces, minGap, linkedToPrevious)) candidates[count++] = index
        }
        return count
    }

    private fun canLink(index: Int, pieces: Int, minGap: Int, linkedToPrevious: BooleanArray): Boolean {
        for (other in max(1, index - minGap)..min(pieces - 1, index + minGap)) if (linkedToPrevious[other]) return false
        return true
    }
}
