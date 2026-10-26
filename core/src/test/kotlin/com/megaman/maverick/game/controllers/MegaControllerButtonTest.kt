package com.megaman.maverick.game.controllers

import com.maverick.game.engine.input.checkInputButtons
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class MegaControllerButtonTest : DescribeSpec({

    describe("MegaControllerButton") {
        it("has each index equal to its ordinal, so the frame bits stay where they were") {
            MegaControllerButton.entries.forEach { it.index shouldBe it.ordinal }
        }

        it("passes the input button check") {
            checkInputButtons(MegaControllerButton.entries)
        }
    }
})
