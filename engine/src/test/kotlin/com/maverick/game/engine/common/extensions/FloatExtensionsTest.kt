package com.maverick.game.engine.common.extensions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

class FloatExtensionsTest : DescribeSpec({

    describe("Float.round") {

        it("rounds half up to the given number of decimals") {
            1.234f.round(2) shouldBe 1.23f
            1.235f.round(1) shouldBe 1.2f
            0.45f.round(2) shouldBe 0.45f
            2.5f.round(0) shouldBe 3f
        }

        it("does not throw in a comma-decimal locale") {
            val default = Locale.getDefault()
            try {
                Locale.setDefault(Locale.GERMANY)
                1.5f.round(2) shouldBe 1.5f
            } finally {
                Locale.setDefault(default)
            }
        }
    }

    describe("Float.roundAngleToStep") {

        it("rounds to the nearest multiple of the step") {
            26.6f.roundAngleToStep(45f) shouldBe 45f
            22.4f.roundAngleToStep(45f) shouldBe 0f
            153.4f.roundAngleToStep(45f) shouldBe 135f
            63.4f.roundAngleToStep(45f) shouldBe 45f
            100f.roundAngleToStep(22.5f) shouldBe 90f
            91f.roundAngleToStep(90f) shouldBe 90f
        }

        it("keeps an angle that is already a multiple") {
            listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f).forEach { it.roundAngleToStep(45f) shouldBe it }
        }

        it("gives 0, not 360, near a full turn") {
            350f.roundAngleToStep(45f) shouldBe 0f
            359.9f.roundAngleToStep(90f) shouldBe 0f
            360f.roundAngleToStep(45f) shouldBe 0f
        }

        it("wraps a negative angle or an angle past a full turn into [0, 360)") {
            (-45f).roundAngleToStep(45f) shouldBe 315f
            (-10f).roundAngleToStep(45f) shouldBe 0f
            (-100f).roundAngleToStep(90f) shouldBe 270f
            400f.roundAngleToStep(45f) shouldBe 45f
            (-720f).roundAngleToStep(45f) shouldBe 0f
        }

        it("fails fast on a step that is not greater than 0") {
            shouldThrow<IllegalArgumentException> { 10f.roundAngleToStep(0f) }
            shouldThrow<IllegalArgumentException> { 10f.roundAngleToStep(-45f) }
        }

        it("fails fast on a step that does not divide 360") {
            shouldThrow<IllegalArgumentException> { 10f.roundAngleToStep(50f) }
            shouldThrow<IllegalArgumentException> { 10f.roundAngleToStep(7f) }
        }
    }
})
