package com.megaman.maverick.game.drawables.ui

import com.megaman.maverick.game.screens.levels.stats.BossHealthHandler
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private data class Runs(val lowRegion: Int, val lowCount: Int, val highRegion: Int, val highCount: Int)

private fun runs(phase: Int, phases: Int, count: Int, max: Int = 30) =
    BossBitsBar.runs(phase, phases, count, max, BossBitRuns()).let {
        Runs(it.lowRegion, it.lowCount, it.highRegion, it.highCount)
    }

class BossBitsBarTest : DescribeSpec({

    describe("the bit runs of a 1-phase boss") {
        it("draw the health in the phase-1 region over black") {
            runs(phase = 1, phases = 1, count = 30) shouldBe Runs(0, 30, BossBitRuns.NO_REGION, 0)
            runs(phase = 1, phases = 1, count = 12) shouldBe Runs(0, 12, BossBitRuns.NO_REGION, 0)
            runs(phase = 1, phases = 1, count = 0) shouldBe Runs(0, 0, BossBitRuns.NO_REGION, 0)
        }
    }

    describe("the bit runs of a 2-phase boss") {
        it("fill the lost bits of phase 1 with the phase-2 region") {
            runs(phase = 1, phases = 2, count = 30) shouldBe Runs(0, 30, 1, 0)
            runs(phase = 1, phases = 2, count = 18) shouldBe Runs(0, 18, 1, 12)
            runs(phase = 1, phases = 2, count = 0) shouldBe Runs(0, 0, 1, 30)
        }

        it("draw the last phase over black") {
            runs(phase = 2, phases = 2, count = 30) shouldBe Runs(1, 30, BossBitRuns.NO_REGION, 0)
            runs(phase = 2, phases = 2, count = 9) shouldBe Runs(1, 9, BossBitRuns.NO_REGION, 0)
            runs(phase = 2, phases = 2, count = 0) shouldBe Runs(1, 0, BossBitRuns.NO_REGION, 0)
        }

        it("clamp a count outside 0 to max") {
            runs(phase = 1, phases = 2, count = 40) shouldBe Runs(0, 30, 1, 0)
            runs(phase = 1, phases = 2, count = -3) shouldBe Runs(0, 0, 1, 30)
        }
    }

    describe("the intro fill") {
        fun fill(phases: Int, max: Int = 30) = (0 until max * phases).map { bit ->
            val phase = BossHealthHandler.phaseAtFillBit(bit, max, phases)
            runs(phase, phases, BossHealthHandler.countAtFillBit(bit, max), max)
        }

        it("of a 2-phase boss fills the phase-2 color over black first, then the phase-1 color over it") {
            val steps = fill(phases = 2)
            steps.size shouldBe 60
            steps.take(30) shouldBe (1..30).map { Runs(1, it, BossBitRuns.NO_REGION, 0) }
            steps.drop(30) shouldBe (1..30).map { Runs(0, it, 1, 30 - it) }
        }

        it("of a 1-phase boss fills the phase-1 color over black, one bit at each step") {
            fill(phases = 1) shouldBe (1..30).map { Runs(0, it, BossBitRuns.NO_REGION, 0) }
        }
    }
})
