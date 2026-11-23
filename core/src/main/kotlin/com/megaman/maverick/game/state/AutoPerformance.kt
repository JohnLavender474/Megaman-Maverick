package com.megaman.maverick.game.state

import com.maverick.game.engine.common.time.Timer
import kotlin.math.min
import kotlin.math.roundToInt

class AutoPerformance {

    companion object {
        private const val HITCH_FRAME_DUR = 0.25f
        private const val FRAME_RATE_WINDOW_DUR = 1f
        private const val NO_READING = -1
        private const val SUSTAINED_DUR = 3f
        private const val GRACE_DUR = 5f
        private const val NEAR_DEFICIT = 5
        private const val MODERATE_DEFICIT = 10
        private const val NEAR_SPEED = 0.3f
        private const val MODERATE_SPEED = 0.5f
        private const val HIGH_SPEED = 1.0f
        private const val CRISIS_SPEED = 2.0f
    }

    private val sustainedTimer = Timer(SUSTAINED_DUR)
    private val graceTimer = Timer(GRACE_DUR)
    private var windowDuration = 0f
    private var windowFrames = 0
    private var measuredFps = NO_READING

    fun update(delta: Float, performance: Performance): Performance? {
        // TODO: a window that the OS slows to about 1 frame/s while it is hidden counts as a slow machine and can drop the tier (see docs/issues/2026-11-23-auto-perf-drops-tier-after-freeze.md)
        val frameTime = min(delta, HITCH_FRAME_DUR)

        measureFrame(frameTime)
        graceTimer.update(frameTime)

        val currentFps = measuredFps
        if (!graceTimer.isFinished() || currentFps == NO_READING || performance == Performance.VERY_LOW ||
            currentFps >= performance.fps
        ) {
            sustainedTimer.reset()
            return null
        }

        val deficit = performance.fps - currentFps
        val lowerPerformance = Performance.entries[performance.ordinal - 1]
        val timerMultiplier = when {
            deficit <= NEAR_DEFICIT -> NEAR_SPEED
            deficit <= MODERATE_DEFICIT -> MODERATE_SPEED
            currentFps > lowerPerformance.fps -> HIGH_SPEED
            else -> CRISIS_SPEED
        }
        sustainedTimer.update(frameTime * timerMultiplier)
        if (!sustainedTimer.isFinished()) return null

        sustainedTimer.reset()
        return lowerPerformance
    }

    fun restartGracePeriod() {
        graceTimer.reset()
        sustainedTimer.reset()
    }

    private fun measureFrame(frameTime: Float) {
        windowDuration += frameTime
        windowFrames++
        if (windowDuration < FRAME_RATE_WINDOW_DUR) return

        measuredFps = (windowFrames / windowDuration).roundToInt()
        windowFrames = 0
        windowDuration = 0f
    }
}
