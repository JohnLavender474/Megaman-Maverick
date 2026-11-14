package com.maverick.game.engine.common.extensions

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
})
