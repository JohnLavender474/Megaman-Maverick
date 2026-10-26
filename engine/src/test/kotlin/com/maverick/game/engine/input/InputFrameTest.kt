package com.maverick.game.engine.input

import com.maverick.game.engine.controller.buttons.ButtonStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private data class TestButton(override val index: Int) : InputButton

class InputFrameTest : DescribeSpec({

    val buttons = (0 until InputFrame.MAX_BUTTONS).map { TestButton(it) }

    describe("InputFrame") {
        it("reads back each status at each index, with the other buttons set to another status") {
            ButtonStatus.entries.forEach { status ->
                ButtonStatus.entries.filter { it != status }.forEach { other ->
                    buttons.forEach { button ->
                        var frame = InputFrame.RELEASED
                        buttons.forEach { frame = frame.with(it, if (it == button) status else other) }

                        buttons.forEach {
                            frame.getStatus(it) shouldBe (if (it == button) status else other)
                        }
                    }
                }
            }
        }

        it("gives RELEASED and false for every query at every index in the default frame") {
            buttons.forEach {
                InputFrame.RELEASED.getStatus(it) shouldBe ButtonStatus.RELEASED
                InputFrame.RELEASED.isPressed(it) shouldBe false
                InputFrame.RELEASED.isJustPressed(it) shouldBe false
                InputFrame.RELEASED.isJustReleased(it) shouldBe false
            }
        }

        it("answers each query by its status") {
            val last = buttons.last()
            ButtonStatus.entries.forEach { status ->
                val frame = InputFrame.RELEASED.with(last, status)

                frame.isPressed(last) shouldBe (status == ButtonStatus.PRESSED || status == ButtonStatus.JUST_PRESSED)
                frame.isJustPressed(last) shouldBe (status == ButtonStatus.JUST_PRESSED)
                frame.isJustReleased(last) shouldBe (status == ButtonStatus.JUST_RELEASED)
            }
        }

        it("changes only the given button with with") {
            val frame = InputFrame.RELEASED.with(buttons[31], ButtonStatus.JUST_PRESSED)

            frame.with(buttons[31], ButtonStatus.RELEASED) shouldBe InputFrame.RELEASED
            buttons.dropLast(1).forEach { frame.getStatus(it) shouldBe ButtonStatus.RELEASED }
        }
    }

    describe("checkInputButtons") {
        it("accepts unique indexes from 0 to 31") {
            checkInputButtons(buttons)
        }

        it("refuses a duplicate index") {
            shouldThrow<IllegalArgumentException> { checkInputButtons(listOf(TestButton(3), TestButton(3))) }
        }

        it("refuses an index below 0") {
            shouldThrow<IllegalArgumentException> { checkInputButtons(listOf(TestButton(-1))) }
        }

        it("refuses an index of 32 or more") {
            shouldThrow<IllegalArgumentException> { checkInputButtons(listOf(TestButton(InputFrame.MAX_BUTTONS))) }
        }
    }
})
