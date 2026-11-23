package com.maverick.game.engine.common.shapes

import com.badlogic.gdx.math.Vector2
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CeilingProfileTest : DescribeSpec({

    val v = floatArrayOf(0f, 32f, 32f, 0f, 64f, 32f)
    val twoTeeth = floatArrayOf(0f, 32f, 32f, 0f, 64f, 32f, 96f, 0f, 128f, 32f)
    val flatBottom = floatArrayOf(0f, 32f, 32f, 0f, 96f, 0f, 128f, 32f)
    val out = Vector2()

    describe("lowestCeiling") {
        it("gives the apex when the span holds it") {
            CeilingProfile.lowestCeiling(v, 3, 16f, 48f) shouldBe 0f
        }

        it("gives the lower span end on one face") {
            CeilingProfile.lowestCeiling(v, 3, 4f, 20f) shouldBe 12f
            CeilingProfile.lowestCeiling(v, 3, 40f, 60f) shouldBe 8f
        }

        it("clamps a span past the chain ends") {
            CeilingProfile.lowestCeiling(v, 3, -20f, 8f) shouldBe 24f
            CeilingProfile.lowestCeiling(v, 3, 56f, 100f) shouldBe 24f
        }

        it("gives no ceiling for a span outside the chain") {
            CeilingProfile.lowestCeiling(v, 3, 70f, 90f) shouldBe Float.POSITIVE_INFINITY
        }

        it("finds the lowest tooth of two") {
            CeilingProfile.lowestCeiling(twoTeeth, 5, 40f, 100f) shouldBe 0f
            CeilingProfile.lowestCeiling(twoTeeth, 5, 48f, 80f) shouldBe 16f
        }

        it("reads a flat bottom") {
            CeilingProfile.lowestCeiling(flatBottom, 4, 40f, 80f) shouldBe 0f
            CeilingProfile.lowestCeiling(flatBottom, 4, 100f, 120f) shouldBe 4f
        }

        it("uses only the first count vertices") {
            val padded = floatArrayOf(0f, 32f, 32f, 0f, 64f, 32f, 96f, -100f)
            CeilingProfile.lowestCeiling(padded, 3, 50f, 64f) shouldBe 18f
        }
    }

    describe("spanBelow") {
        it("gives the x interval where the chain is below y") {
            CeilingProfile.spanBelow(v, 3, 16f, out) shouldBe true
            out shouldBe Vector2(16f, 48f)
        }

        it("gives the chain ends when the whole chain is below y") {
            CeilingProfile.spanBelow(v, 3, 40f, out) shouldBe true
            out shouldBe Vector2(0f, 64f)
        }

        it("gives false when no point is below y") {
            CeilingProfile.spanBelow(v, 3, 0f, out) shouldBe false
        }

        it("reads a flat bottom") {
            CeilingProfile.spanBelow(flatBottom, 4, 8f, out) shouldBe true
            out shouldBe Vector2(24f, 104f)
        }
    }
})
