package com.megaman.maverick.game.input

import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.controllers.MegaControllerButton
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PlayerInputsTest : DescribeSpec({

    describe("PlayerInputs") {
        it("starts with the default frame in every slot and stores a frame per slot") {
            val inputs = PlayerInputs(4)
            (0 until 4).forEach { inputs[PlayerSlot(it)] shouldBe InputFrame.RELEASED }

            val frame = InputFrame.RELEASED.with(MegaControllerButton.START, ButtonStatus.JUST_PRESSED)
            inputs[PlayerSlot(2)] = frame

            inputs[PlayerSlot(2)] shouldBe frame
            inputs[PlayerSlot(0)] shouldBe InputFrame.RELEASED
        }
    }
})
