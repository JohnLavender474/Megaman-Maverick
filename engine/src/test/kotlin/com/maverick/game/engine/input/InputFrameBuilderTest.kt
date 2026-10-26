package com.maverick.game.engine.input

import com.maverick.game.engine.controller.buttons.ButtonStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private data class BuilderTestButton(override val index: Int) : InputButton

class InputFrameBuilderTest : DescribeSpec({

    val a = BuilderTestButton(0)
    val b = BuilderTestButton(31)
    val bits = 1L shl b.index

    describe("InputFrameBuilder") {
        it("starts with the default frame") {
            InputFrameBuilder(listOf(a, b)).frame shouldBe InputFrame.RELEASED
        }

        it("goes RELEASED, JUST_PRESSED, PRESSED, JUST_RELEASED, RELEASED as a button is held and let go") {
            val builder = InputFrameBuilder(listOf(a, b))

            builder.build(0L).getStatus(b) shouldBe ButtonStatus.RELEASED
            builder.build(bits).getStatus(b) shouldBe ButtonStatus.JUST_PRESSED
            builder.build(bits).getStatus(b) shouldBe ButtonStatus.PRESSED
            builder.build(0L).getStatus(b) shouldBe ButtonStatus.JUST_RELEASED
            builder.build(0L).getStatus(b) shouldBe ButtonStatus.RELEASED
        }

        it("gives JUST_PRESSED again after one released tick") {
            val builder = InputFrameBuilder(listOf(b))

            builder.build(bits)
            builder.build(0L).getStatus(b) shouldBe ButtonStatus.JUST_RELEASED
            builder.build(bits).getStatus(b) shouldBe ButtonStatus.JUST_PRESSED
        }

        it("reads only the bit of each button") {
            val builder = InputFrameBuilder(listOf(a, b))

            val frame = builder.build(bits)

            frame.getStatus(a) shouldBe ButtonStatus.RELEASED
            frame.getStatus(b) shouldBe ButtonStatus.JUST_PRESSED
        }

        it("while locked, gives JUST_RELEASED once and then RELEASED for a held button") {
            val builder = InputFrameBuilder(listOf(b))
            builder.build(bits)
            builder.build(bits)

            builder.locked = true

            builder.build(bits).getStatus(b) shouldBe ButtonStatus.JUST_RELEASED
            builder.build(bits).getStatus(b) shouldBe ButtonStatus.RELEASED
        }

        it("while locked, keeps a released button RELEASED and ignores a new press") {
            val builder = InputFrameBuilder(listOf(b))
            builder.locked = true

            builder.build(bits).getStatus(b) shouldBe ButtonStatus.RELEASED
        }

        it("gives JUST_PRESSED on unlock for a button that is still held") {
            val builder = InputFrameBuilder(listOf(b))
            builder.locked = true
            builder.build(bits)

            builder.locked = false

            builder.build(bits).getStatus(b) shouldBe ButtonStatus.JUST_PRESSED
        }

        it("keeps the last frame in frame") {
            val builder = InputFrameBuilder(listOf(b))

            val frame = builder.build(bits)

            builder.frame shouldBe frame
        }
    }
})
