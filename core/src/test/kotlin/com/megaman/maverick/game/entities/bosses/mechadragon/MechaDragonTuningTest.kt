package com.megaman.maverick.game.entities.bosses.mechadragon

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class MechaDragonTuningTest : DescribeSpec({

    describe("the tuning rows") {
        it("normal: one sweep high to low, 0.75 s breath tell, 1.2 s sweep") {
            val row = MechaDragonTuning.of(hard = false, panic = false)
            row.returnSweep shouldBe false
            row.breathWindupDur shouldBe 0.75f
            row.sweepDur shouldBe 1.2f
        }

        it("hard: shorter tells and sweep, and always the return sweep") {
            val normal = MechaDragonTuning.of(hard = false, panic = false)
            val hard = MechaDragonTuning.of(hard = true, panic = false)
            hard.returnSweep shouldBe true
            (hard.breathWindupDur < normal.breathWindupDur) shouldBe true
            (hard.sweepDur < normal.sweepDur) shouldBe true
            (hard.landHangDur < normal.landHangDur) shouldBe true
            (hard.chompTellDur < normal.chompTellDur) shouldBe true
            (hard.chargeTellMaxDur < normal.chargeTellMaxDur) shouldBe true
        }

        it("panic: the return sweep and tells 20% shorter, the sweep speed unchanged, in both difficulties") {
            listOf(false, true).forEach { hard ->
                val base = MechaDragonTuning.of(hard = hard, panic = false)
                val panic = MechaDragonTuning.of(hard = hard, panic = true)
                panic.returnSweep shouldBe true
                panic.sweepDur shouldBe base.sweepDur
                panic.breathWindupDur shouldBe (base.breathWindupDur * 0.8f plusOrMinus 0.0001f)
                panic.landHangDur shouldBe (base.landHangDur * 0.8f plusOrMinus 0.0001f)
                panic.chompTellDur shouldBe (base.chompTellDur * 0.8f plusOrMinus 0.0001f)
                panic.chargeTellMinDur shouldBe (base.chargeTellMinDur * 0.8f plusOrMinus 0.0001f)
            }
        }

        it("keeps the old charge wait of 1 to 2 s in normal mode") {
            val row = MechaDragonTuning.of(hard = false, panic = false)
            row.chargeTellMinDur shouldBe 1f
            row.chargeTellMaxDur shouldBe 2f
        }
    }
})
