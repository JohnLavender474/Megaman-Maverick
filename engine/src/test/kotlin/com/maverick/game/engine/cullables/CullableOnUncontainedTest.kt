package com.maverick.game.engine.cullables

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CullableOnUncontainedTest :
    DescribeSpec({
        describe("CullableOnUncontained") {
            it("should be culled when not contained") {
                val container = "within"
                var containable = "in"

                val cullableOnUncontained =
                    com.maverick.game.engine.cullables.CullableOnUncontained({ container }, { containable in it }, 0f)

                cullableOnUncontained.shouldBeCulled(1f) shouldBe false

                // Update the container to contain the containable
                containable = "out"
                cullableOnUncontained.shouldBeCulled(1f) shouldBe true

                // Reset the container to not contain the containable
                containable = "in"
                cullableOnUncontained.reset()
                cullableOnUncontained.shouldBeCulled(1f) shouldBe false
            }

            it("clears the time out of the container at reset, as a new cullable") {
                var containable = "out"
                val cullable = CullableOnUncontained({ "within" }, { containable in it }, 1f)

                cullable.shouldBeCulled(0.75f) shouldBe false
                cullable.reset()
                cullable.shouldBeCulled(0.5f) shouldBe false
                cullable.shouldBeCulled(0.5f) shouldBe true
            }

            it("should not be culled when contained") {
                val container = "within"
                var containable = "in"

                val cullableOnUncontained =
                    com.maverick.game.engine.cullables.CullableOnUncontained({ container }, { containable in it }, 0f)

                cullableOnUncontained.shouldBeCulled(1f) shouldBe false

                // Set the container to contain the containable
                containable = "within"
                cullableOnUncontained.shouldBeCulled(1f) shouldBe false
            }
        }
    })
