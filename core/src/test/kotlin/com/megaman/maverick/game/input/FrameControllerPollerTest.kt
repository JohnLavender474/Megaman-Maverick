package com.megaman.maverick.game.input

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.input.InputFrame
import com.megaman.maverick.game.controllers.MegaControllerButton
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class FrameControllerPollerTest : DescribeSpec({

    describe("FrameControllerPoller") {
        it("answers each query that the menus and skip screens use from the source frame") {
            ButtonStatus.entries.forEach { status ->
                val frame = InputFrame.RELEASED.with(MegaControllerButton.START, status)
                val poller = FrameControllerPoller { frame }
                val start = MegaControllerButton.START
                val buttons = Array<Any>().apply { add(MegaControllerButton.A); add(start) }

                poller.getStatus(start) shouldBe status
                poller.isPressed(start) shouldBe (status == ButtonStatus.PRESSED || status == ButtonStatus.JUST_PRESSED)
                poller.isJustPressed(start) shouldBe (status == ButtonStatus.JUST_PRESSED)
                poller.isJustReleased(start) shouldBe (status == ButtonStatus.JUST_RELEASED)
                poller.isAnyJustPressed(buttons) shouldBe (status == ButtonStatus.JUST_PRESSED)
                poller.isAnyJustReleased(buttons) shouldBe (status == ButtonStatus.JUST_RELEASED)
            }
        }

        it("reads the source again on each query") {
            var frame = InputFrame.RELEASED
            val poller = FrameControllerPoller { frame }

            poller.isJustPressed(MegaControllerButton.A) shouldBe false

            frame = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)

            poller.isJustPressed(MegaControllerButton.A) shouldBe true
        }

        it("gives no status for a key that is not a game button") {
            FrameControllerPoller { InputFrame.RELEASED }.getStatus("other") shouldBe null
        }
    }
})
