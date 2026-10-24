package com.megaman.maverick.game.state

import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GameStateHealthTankTest : DescribeSpec({

    lateinit var state: GameState

    fun tankValues() = MegaHealthTank.entries.associateWith { state.getHealthTankValue(it) }

    beforeEach { state = GameState() }

    describe("addToHealthTanks") {
        it("fills a partially filled tank before an empty tank that comes earlier in enum order") {
            // the bug: B was drained to refill health, C was left half full, and collected health energy
            // went to B (first in enum order) instead of topping off C
            state.putHealthTank(MegaHealthTank.B, 0)
            state.putHealthTank(MegaHealthTank.C, ConstVals.MAX_HEALTH / 2)

            state.addToHealthTanks(5) shouldBe true

            tankValues() shouldBe mapOf(
                MegaHealthTank.A to 0,
                MegaHealthTank.B to 0,
                MegaHealthTank.C to (ConstVals.MAX_HEALTH / 2) + 5,
                MegaHealthTank.D to 0
            )
        }

        it("skips full tanks and fills the fullest tank that still has room") {
            state.putHealthTank(MegaHealthTank.A, ConstVals.MAX_HEALTH)
            state.putHealthTank(MegaHealthTank.B, 2)
            state.putHealthTank(MegaHealthTank.C, 20)

            state.addToHealthTanks(5) shouldBe true

            tankValues() shouldBe mapOf(
                MegaHealthTank.A to ConstVals.MAX_HEALTH,
                MegaHealthTank.B to 2,
                MegaHealthTank.C to 25,
                MegaHealthTank.D to 0
            )
        }

        it("overflows into the next fullest tank once the fullest one is topped off") {
            state.putHealthTank(MegaHealthTank.B, 0)
            state.putHealthTank(MegaHealthTank.C, ConstVals.MAX_HEALTH - 4)

            state.addToHealthTanks(10) shouldBe true

            tankValues() shouldBe mapOf(
                MegaHealthTank.A to 0,
                MegaHealthTank.B to 6,
                MegaHealthTank.C to ConstVals.MAX_HEALTH,
                MegaHealthTank.D to 0
            )
        }

        it("falls back to enum order when all owned tanks are equally filled") {
            state.putHealthTank(MegaHealthTank.B, 0)
            state.putHealthTank(MegaHealthTank.D, 0)

            state.addToHealthTanks(5) shouldBe true

            tankValues() shouldBe mapOf(
                MegaHealthTank.A to 0,
                MegaHealthTank.B to 5,
                MegaHealthTank.C to 0,
                MegaHealthTank.D to 0
            )
        }

        it("discards excess health once every owned tank is full") {
            state.putHealthTank(MegaHealthTank.A, ConstVals.MAX_HEALTH - 1)

            state.addToHealthTanks(10) shouldBe true

            tankValues() shouldBe mapOf(
                MegaHealthTank.A to ConstVals.MAX_HEALTH,
                MegaHealthTank.B to 0,
                MegaHealthTank.C to 0,
                MegaHealthTank.D to 0
            )
        }

        it("returns false when no owned tank has room") {
            state.putHealthTank(MegaHealthTank.A, ConstVals.MAX_HEALTH)

            state.addToHealthTanks(5) shouldBe false
        }

        it("returns false when no tanks are owned") {
            state.addToHealthTanks(5) shouldBe false
        }
    }

    describe("hasAnyHealthTanks") {
        it("is false with no tanks and true with one") {
            state.hasAnyHealthTanks() shouldBe false
            state.putHealthTank(MegaHealthTank.C)
            state.hasAnyHealthTanks() shouldBe true
        }
    }
})
