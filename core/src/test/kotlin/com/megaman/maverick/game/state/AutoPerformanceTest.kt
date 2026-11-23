package com.megaman.maverick.game.state

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val FRAME_60_FPS = 1f / 60f
private const val FRAME_30_FPS = 1f / 30f
private const val FRAME_20_FPS = 1f / 20f
private const val FRAME_10_FPS = 1f / 10f
private const val FRAME_3_FPS = 1f / 3f

private fun AutoPerformance.runFrames(frameDelta: Float, duration: Float, performance: Performance): Performance? {
    var elapsed = 0f
    while (elapsed < duration) {
        val lowerPerformance = update(frameDelta, performance)
        if (lowerPerformance != null) return lowerPerformance
        elapsed += frameDelta
    }
    return null
}

class AutoPerformanceTest : DescribeSpec({

    describe("AutoPerformance") {
        it("never drops the tier at a steady 60 fps at MEDIUM") {
            AutoPerformance().runFrames(FRAME_60_FPS, 30f, Performance.MEDIUM).shouldBeNull()
        }

        it("drops MEDIUM to LOW at a steady 30 fps") {
            AutoPerformance().runFrames(FRAME_30_FPS, 10f, Performance.MEDIUM) shouldBe Performance.LOW
        }

        it("never drops the tier at VERY_LOW") {
            AutoPerformance().runFrames(FRAME_10_FPS, 30f, Performance.VERY_LOW).shouldBeNull()
        }

        listOf(0.6f, 1f, 1.5f, 3f, 10f).forEach { freeze ->
            it("does not drop the tier after one freeze of $freeze s between 60 fps frames") {
                val autoPerformance = AutoPerformance()
                autoPerformance.runFrames(FRAME_60_FPS, 10f, Performance.MEDIUM).shouldBeNull()
                (0 until 30).forEach { offset ->
                    autoPerformance.runFrames(FRAME_60_FPS, offset / 30f, Performance.MEDIUM).shouldBeNull()
                    autoPerformance.update(freeze, Performance.MEDIUM).shouldBeNull()
                    autoPerformance.runFrames(FRAME_60_FPS, 5f, Performance.MEDIUM).shouldBeNull()
                }
            }
        }

        it("drops MEDIUM to LOW on a machine whose frames all take over 0.25 s") {
            AutoPerformance().runFrames(FRAME_3_FPS, 15f, Performance.MEDIUM) shouldBe Performance.LOW
        }

        it("does not drop the tier after two freezes in a row between 60 fps frames") {
            val autoPerformance = AutoPerformance()
            autoPerformance.runFrames(FRAME_60_FPS, 10f, Performance.MEDIUM).shouldBeNull()
            (0 until 30).forEach { offset ->
                autoPerformance.runFrames(FRAME_60_FPS, offset / 30f, Performance.MEDIUM).shouldBeNull()
                repeat(2) { autoPerformance.update(3f, Performance.MEDIUM).shouldBeNull() }
                autoPerformance.runFrames(FRAME_60_FPS, 5f, Performance.MEDIUM).shouldBeNull()
            }
        }

        it("drops MEDIUM to LOW on a steady stutter of 0.3 s frames between 60 fps frames") {
            val autoPerformance = AutoPerformance()
            var lowerPerformance: Performance? = null
            var elapsed = 0f
            while (lowerPerformance == null && elapsed < 30f) {
                lowerPerformance = autoPerformance.update(0.3f, Performance.MEDIUM)
                    ?: autoPerformance.update(FRAME_60_FPS, Performance.MEDIUM)
                elapsed += 0.3f + FRAME_60_FPS
            }
            lowerPerformance shouldBe Performance.LOW
        }

        it("does not let a freeze use up the grace period") {
            val autoPerformance = AutoPerformance()
            autoPerformance.runFrames(FRAME_30_FPS, 2f, Performance.MEDIUM).shouldBeNull()
            autoPerformance.restartGracePeriod()
            autoPerformance.update(10f, Performance.MEDIUM).shouldBeNull()
            autoPerformance.runFrames(FRAME_30_FPS, 4.9f, Performance.MEDIUM).shouldBeNull()
        }

        it("still drops the tier at a steady 30 fps with a freeze in between") {
            val autoPerformance = AutoPerformance()
            autoPerformance.runFrames(FRAME_30_FPS, 5.5f, Performance.MEDIUM).shouldBeNull()
            autoPerformance.update(2f, Performance.MEDIUM).shouldBeNull()
            autoPerformance.runFrames(FRAME_30_FPS, 5f, Performance.MEDIUM) shouldBe Performance.LOW
        }

        it("waits for the grace period after a restart before it drops again") {
            val autoPerformance = AutoPerformance()
            autoPerformance.runFrames(FRAME_30_FPS, 10f, Performance.LOW) shouldBe Performance.VERY_LOW
            autoPerformance.restartGracePeriod()
            autoPerformance.runFrames(FRAME_20_FPS, 4.9f, Performance.LOW).shouldBeNull()
        }
    }
})
