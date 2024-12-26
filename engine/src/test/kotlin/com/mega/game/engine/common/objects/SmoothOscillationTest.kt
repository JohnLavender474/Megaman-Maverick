package com.mega.game.engine.common.objects

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SmoothOscillationTest : DescribeSpec({
    describe("SmoothOscillationTimerTest") {
        describe("min = 0, max = 1") {
            it("should equal 0") {
                val timer = SmoothOscillationTimer(duration = 2f, start = 0f, end = 1f)
                timer.getValue() shouldBe 0f
            }

            it("should equal 0.5") {
                val timer = SmoothOscillationTimer(duration = 2f, start = 0f, end = 1f)
                timer.update(0.5f)
                timer.getValue() shouldBe 0.5f
            }

            it("should equal 1") {
                val timer = SmoothOscillationTimer(duration = 2f, start = 0f, end = 1f)
                timer.update(1f)
                timer.getValue() shouldBe 1f
            }

            it("should equal 0.5") {
                val timer = SmoothOscillationTimer(duration = 2f, start = 0f, end = 1f)
                timer.update(1.5f)
                timer.getValue() shouldBe 0.5f
            }

            it("should equal 0") {
                val timer = SmoothOscillationTimer(duration = 2f, start = 0f, end = 1f)
                timer.update(2f)
                timer.getValue() shouldBe 0f
            }
        }
        describe("min = -1, max = 1") {
            it("should equal -1") {
                val timer = SmoothOscillationTimer(duration = 1f, start = -1f, end = 1f)
                timer.getValue() shouldBe -1f
            }

            it("should equal 0") {
                val timer = SmoothOscillationTimer(duration = 1f, start = -1f, end = 1f)
                timer.update(0.25f)
                timer.getValue() shouldBe 0
            }

            it("should equal 1") {
                val timer = SmoothOscillationTimer(duration = 1f, start = -1f, end = 1f)
                timer.update(0.5f)
                timer.getValue() shouldBe 1f
            }

            it("should equal 0") {
                val timer = SmoothOscillationTimer(duration = 1f, start = -1f, end = 1f)
                timer.update(0.75f)
                timer.getValue() shouldBe 0
            }

            it("should equal -1") {
                val timer = SmoothOscillationTimer(duration = 1f, start = -1f, end = 1f)
                timer.update(1f)
                timer.getValue() shouldBe -1f
            }
        }
    }
})