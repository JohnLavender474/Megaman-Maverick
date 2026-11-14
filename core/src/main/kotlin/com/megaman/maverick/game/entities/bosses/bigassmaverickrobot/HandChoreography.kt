package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.megaman.maverick.game.ConstVals

enum class PartnerReaction { NONE, BRACE, COUNTER, SETTLE, LIFT, COIL, DIP, JOLT }

object HandChoreography {

    const val BRACE_DOWN = 1f
    const val BRACE_IN = 0.5f
    const val COUNTER_DISTANCE = 0.75f
    const val COUNTER_PEAK = 0.15f
    const val LIFT_UP = 0.75f
    const val LIFT_DUR = 0.3f
    const val DIP_DOWN = 1f
    const val DIP_DUR = 0.1f
    const val JOLT_UP = 0.25f
    const val JOLT_DUR = 0.2f
    const val RIDDEN_SCALE = 0.25f
    const val SHAKE = 2f
    const val SHAKE_PERIOD = 0.1f

    fun reactionTo(attackerState: HandState) = when (attackerState) {
        HandState.WIND_UP -> PartnerReaction.BRACE
        HandState.LAUNCH -> PartnerReaction.COUNTER
        HandState.RECOIL -> PartnerReaction.SETTLE
        HandState.RISE -> PartnerReaction.LIFT
        HandState.HANG -> PartnerReaction.COIL
        HandState.FALL -> PartnerReaction.DIP
        HandState.REST -> PartnerReaction.JOLT
        else -> PartnerReaction.NONE
    }

    fun shake(time: Float) = if (MathUtils.sin(MathUtils.PI2 * time / SHAKE_PERIOD) >= 0f) SHAKE else -SHAKE

    fun partnerOffset(
        attackerState: HandState,
        timeInState: Float,
        stateDur: Float,
        attackDirection: Vector2,
        towardHead: Float,
        ridden: Boolean,
        scale: Float,
        out: Vector2
    ): Vector2 {
        when (reactionTo(attackerState)) {
            PartnerReaction.BRACE -> {
                val t = Interpolation.smooth.apply(progress(timeInState, stateDur))
                out.set(towardHead * BRACE_IN * t, -BRACE_DOWN * t)
            }

            PartnerReaction.COUNTER -> {
                val t = Interpolation.smooth.apply(progress(timeInState, COUNTER_PEAK))
                out.set(attackDirection).nor().scl(-COUNTER_DISTANCE * t)
            }

            PartnerReaction.SETTLE -> {
                val t = 1f - Interpolation.smooth.apply(progress(timeInState, stateDur))
                out.set(attackDirection).nor().scl(-COUNTER_DISTANCE * t)
            }

            PartnerReaction.LIFT -> out.set(0f, LIFT_UP * Interpolation.smooth.apply(progress(timeInState, LIFT_DUR)))
            PartnerReaction.COIL -> out.set(shake(timeInState) / ConstVals.PPM, LIFT_UP)
            PartnerReaction.DIP -> out.set(
                0f, MathUtils.lerp(LIFT_UP, DIP_DOWN * attackDirection.y, progress(timeInState, DIP_DUR))
            )
            PartnerReaction.JOLT -> out.set(
                0f, if (timeInState < JOLT_DUR) JOLT_UP * MathUtils.sin(MathUtils.PI * timeInState / JOLT_DUR) else 0f
            )

            PartnerReaction.NONE -> out.setZero()
        }
        return out.scl(scale * (if (ridden) RIDDEN_SCALE else 1f) * ConstVals.PPM)
    }

    private fun progress(time: Float, dur: Float) = if (dur <= 0f) 1f else MathUtils.clamp(time / dur, 0f, 1f)
}
