package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.ConstVals

enum class MotionMode { ORBIT, HOVER, BLEND }

enum class HandSide(val startDegrees: Float, val turn: Float, val hoverPhase: Float) {
    LEFT(180f, -1f, 0f),
    RIGHT(0f, 1f, 0.25f)
}

class BigAssMaverickRobotMotion {

    companion object {
        const val ORBIT_RADIUS = 2f
        const val ORBIT_MIN_SPEED = 160f
        const val ORBIT_MAX_SPEED = 256f
        const val ORBIT_CYCLE = 360f
        const val HOVER_AMPLITUDE_X = 1f
        const val HOVER_AMPLITUDE_Y = 0.75f
        const val HOVER_PERIOD_X = 3f
        const val HOVER_PERIOD_Y = 2f
        const val HOVER_CYCLE = 6f
        const val BLEND_MAX_DUR = 1f
        const val ARRIVED_DISTANCE = 0.25f
        const val SPRING_W0 = 8f
        const val SPRING_MAX_SPEED = 8f

        fun orbitSlot(origin: Vector2, side: HandSide, traveledDegrees: Float, out: Vector2): Vector2 {
            val degrees = side.startDegrees + side.turn * traveledDegrees
            val radius = ORBIT_RADIUS * ConstVals.PPM
            return out.set(origin.x + radius * MathUtils.cosDeg(degrees), origin.y + radius * MathUtils.sinDeg(degrees))
        }

        fun hoverSlot(origin: Vector2, side: HandSide, time: Float, amplitudeScale: Float, out: Vector2): Vector2 {
            val phase = MathUtils.PI2 * side.hoverPhase
            val x = HOVER_AMPLITUDE_X * amplitudeScale * MathUtils.sin(MathUtils.PI2 * time / HOVER_PERIOD_X + phase)
            val y = HOVER_AMPLITUDE_Y * amplitudeScale * MathUtils.sin(MathUtils.PI2 * time / HOVER_PERIOD_Y + phase)
            return out.set(origin.x + x * ConstVals.PPM, origin.y + y * ConstVals.PPM)
        }

        fun spring(
            position: Vector2,
            velocity: Vector2,
            target: Vector2,
            maxSpeed: Float,
            delta: Float,
            out: Vector2
        ): Vector2 {
            val w0 = SPRING_W0
            val ax = w0 * w0 * (target.x - position.x) - 2f * w0 * velocity.x
            val ay = w0 * w0 * (target.y - position.y) - 2f * w0 * velocity.y
            out.set(velocity.x + ax * delta, velocity.y + ay * delta)
            val max = maxSpeed * ConstVals.PPM
            if (out.len2() > max * max) out.setLength(max)
            return out
        }
    }

    var mode = MotionMode.ORBIT
        private set
    var nextMode = MotionMode.ORBIT
        private set
    var traveledDegrees = 0f
        private set
    var hoverTime = 0f
        private set
    var blendTime = 0f
        private set

    private var schedule: Array<MotionMode>? = null
    private var scheduleIndex = 0

    fun reset() {
        mode = MotionMode.ORBIT
        nextMode = MotionMode.ORBIT
        traveledDegrees = 0f
        hoverTime = 0f
        blendTime = 0f
        schedule = null
        scheduleIndex = 0
    }

    fun update(
        delta: Float,
        orbitSpeed: Float,
        hoverTimeScale: Float,
        schedule: Array<MotionMode>,
        handsArrived: Boolean
    ) {
        if (this.schedule == null) this.schedule = schedule
        when (mode) {
            MotionMode.ORBIT -> {
                traveledDegrees += orbitSpeed * delta
                if (traveledDegrees >= ORBIT_CYCLE) endCycle(schedule)
            }

            MotionMode.HOVER -> {
                hoverTime += hoverTimeScale * delta
                if (hoverTime >= HOVER_CYCLE) endCycle(schedule)
            }

            MotionMode.BLEND -> {
                blendTime += delta
                if (handsArrived || blendTime >= BLEND_MAX_DUR) {
                    mode = nextMode
                    traveledDegrees = 0f
                    hoverTime = 0f
                }
            }
        }
    }

    fun slot(origin: Vector2, side: HandSide, amplitudeScale: Float, out: Vector2): Vector2 = when (mode) {
        MotionMode.ORBIT -> orbitSlot(origin, side, traveledDegrees, out)
        MotionMode.HOVER -> hoverSlot(origin, side, hoverTime, amplitudeScale, out)
        MotionMode.BLEND -> when (nextMode) {
            MotionMode.HOVER -> hoverSlot(origin, side, 0f, amplitudeScale, out)
            else -> orbitSlot(origin, side, 0f, out)
        }
    }

    private fun endCycle(schedule: Array<MotionMode>) {
        scheduleIndex = if (schedule !== this.schedule) 0 else (scheduleIndex + 1) % schedule.size
        this.schedule = schedule
        val next = schedule[scheduleIndex]
        when {
            next == MotionMode.ORBIT && mode == MotionMode.ORBIT -> traveledDegrees -= ORBIT_CYCLE
            next == MotionMode.HOVER && mode == MotionMode.HOVER -> hoverTime -= HOVER_CYCLE
            else -> {
                nextMode = next
                mode = MotionMode.BLEND
                blendTime = 0f
            }
        }
    }
}
