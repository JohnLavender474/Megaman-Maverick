package com.megaman.maverick.game.entities.bosses

import com.megaman.maverick.game.entities.bosses.D1000.D1000Action
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class D1000Test : DescribeSpec({

    val high = D1000.HALF_HEALTH + 1
    val low = D1000.HALF_HEALTH

    describe("chooseAction") {

        it("above half health after a crouch walks for r < 8, else summons") {
            D1000.chooseAction(7, high, afterCrouch = true) shouldBe D1000Action.WALK
            D1000.chooseAction(8, high, afterCrouch = true) shouldBe D1000Action.SUMMON
        }

        it("above half health at other times rams for r < 8, else summons") {
            D1000.chooseAction(7, high, afterCrouch = false) shouldBe D1000Action.RAM
            D1000.chooseAction(8, high, afterCrouch = false) shouldBe D1000Action.SUMMON
        }

        it("at half health or less after a crouch summons for r < 6, else bombs") {
            D1000.chooseAction(5, low, afterCrouch = true) shouldBe D1000Action.SUMMON
            D1000.chooseAction(6, low, afterCrouch = true) shouldBe D1000Action.BOMB
        }

        it("at half health or less at other times rams for r < 6, else bombs") {
            D1000.chooseAction(5, low, afterCrouch = false) shouldBe D1000Action.RAM
            D1000.chooseAction(6, low, afterCrouch = false) shouldBe D1000Action.BOMB
        }

        it("never bombs above half health and never walks at half health or less") {
            (0..15).forEach { r ->
                listOf(true, false).forEach { afterCrouch ->
                    (D1000.chooseAction(r, high, afterCrouch) == D1000Action.BOMB) shouldBe false
                    (D1000.chooseAction(r, low, afterCrouch) == D1000Action.WALK) shouldBe false
                }
            }
        }
    }
})
