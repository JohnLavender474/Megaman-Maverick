package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.extensions.gdxArrayOf

class BigAssMaverickRobotTuning(
    val attackWait: Float,
    val punchWeight: Int,
    val slamWeight: Int,
    val clapWeight: Int,
    val windUpDur: Float,
    val launchMinSpeed: Float,
    val launchMaxSpeed: Float,
    val lockDur: Float,
    val restDur: Float,
    val shockWavePairs: Int,
    val waveLife: Float,
    val clapSpeed: Float,
    val orbAims: Array<OrbAim>,
    val orbGap: Float,
    val volleyDur: Float,
    val volleyDelay: Float,
    val orbMinHold: Float,
    val orbMaxHold: Float,
    val orbMinSpeed: Float,
    val orbMaxSpeed: Float,
    val orbitSpeedScale: Float,
    val hoverTimeScale: Float,
    val hoverAmplitudeScale: Float,
    val partnerScale: Float,
    val schedule: Array<MotionMode>
) {

    fun totalWeight(freeHands: Int) = when {
        freeHands <= 0 -> 0
        freeHands == 1 -> punchWeight + slamWeight
        else -> punchWeight + slamWeight + clapWeight
    }

    fun attackFor(roll: Int, freeHands: Int): HandAttack? {
        if (totalWeight(freeHands) <= 0) return null
        return when {
            roll < punchWeight -> HandAttack.PUNCH
            roll < punchWeight + slamWeight -> HandAttack.SLAM
            freeHands >= 2 -> HandAttack.PINCER_CLAP
            else -> HandAttack.SLAM
        }
    }

    companion object {
        const val ATTACK_WAIT = 3f
        const val ATTACK_WAIT_PHASE_2 = 2.25f
        const val ATTACK_WAIT_HARD = 1.75f
        const val ATTACK_WAIT_HARD_PHASE_2 = 1.25f
        const val WIND_UP_DUR = 0.75f
        const val WIND_UP_DUR_PHASE_2 = 0.6f
        const val WIND_UP_DUR_HARD = 0.55f
        const val WIND_UP_DUR_HARD_PHASE_2 = 0.45f
        const val LAUNCH_MIN_SPEED = 6f
        const val LAUNCH_MAX_SPEED = 9f
        const val LAUNCH_MIN_SPEED_HARD = 8f
        const val LAUNCH_MAX_SPEED_HARD = 12f
        const val LOCK_DUR = 0.5f
        const val LOCK_DUR_PHASE_2 = 0.45f
        const val LOCK_DUR_HARD = 0.4f
        const val LOCK_DUR_HARD_PHASE_2 = 0.35f
        const val REST_DUR = 1.2f
        const val REST_DUR_PHASE_2 = 1f
        const val REST_DUR_HARD = 0.9f
        const val REST_DUR_HARD_PHASE_2 = 0.7f
        const val CLAP_SPEED = 12f
        const val CLAP_SPEED_HARD = 14f
        const val WAVE_LIFE = 0.5f
        const val WAVE_LIFE_PHASE_2 = 0.6f
        const val WAVE_LIFE_HARD = 0.6f
        const val WAVE_LIFE_HARD_PHASE_2 = 0.8f
        const val ORB_GAP = 1f
        const val ORB_GAP_PHASE_2 = 0.9f
        const val ORB_GAP_HARD = 0.6f
        const val ORB_GAP_HARD_PHASE_2 = 0.4f
        const val VOLLEY_DUR = 4f
        const val VOLLEY_DELAY = 3f
        const val VOLLEY_DUR_PHASE_2 = 3f
        const val VOLLEY_DELAY_PHASE_2 = 2f
        const val VOLLEY_DUR_HARD = 2f
        const val VOLLEY_DELAY_HARD = 2f
        const val VOLLEY_DUR_HARD_PHASE_2 = 2f
        const val VOLLEY_DELAY_HARD_PHASE_2 = 1.5f
        const val ORB_MIN_HOLD = 0.25f
        const val ORB_MAX_HOLD = 1f
        const val ORB_HOLD_PHASE_2 = 0.5f
        const val ORB_MIN_HOLD_HARD = 0.1f
        const val ORB_MAX_HOLD_HARD = 0.5f
        const val ORB_HOLD_HARD_PHASE_2 = 0.25f
        const val ORB_MIN_SPEED = 6f
        const val ORB_MAX_SPEED = 10f
        const val ORB_MIN_SPEED_HARD = 8f
        const val ORB_MAX_SPEED_HARD = 12f
        const val PHASE_2_ORBIT_SPEED_SCALE = 1.2f
        const val PHASE_2_HOVER_TIME_SCALE = 1.4f
        const val PHASE_2_HOVER_AMPLITUDE_SCALE = 1.3f
        const val PHASE_2_PARTNER_SCALE = 1.25f

        private val SCHEDULE = gdxArrayOf(MotionMode.ORBIT, MotionMode.ORBIT, MotionMode.HOVER)
        private val SCHEDULE_PHASE_2 = gdxArrayOf(MotionMode.ORBIT, MotionMode.HOVER, MotionMode.HOVER)

        val NORMAL = BigAssMaverickRobotTuning(
            attackWait = ATTACK_WAIT, punchWeight = 2, slamWeight = 1, clapWeight = 0,
            windUpDur = WIND_UP_DUR, launchMinSpeed = LAUNCH_MIN_SPEED, launchMaxSpeed = LAUNCH_MAX_SPEED,
            lockDur = LOCK_DUR, restDur = REST_DUR, shockWavePairs = 1, waveLife = WAVE_LIFE, clapSpeed = CLAP_SPEED,
            orbAims = gdxArrayOf(OrbAim.CURRENT, OrbAim.LEAD), orbGap = ORB_GAP, volleyDur = VOLLEY_DUR,
            volleyDelay = VOLLEY_DELAY, orbMinHold = ORB_MIN_HOLD, orbMaxHold = ORB_MAX_HOLD,
            orbMinSpeed = ORB_MIN_SPEED, orbMaxSpeed = ORB_MAX_SPEED, orbitSpeedScale = 1f, hoverTimeScale = 1f,
            hoverAmplitudeScale = 1f, partnerScale = 1f, schedule = SCHEDULE
        )
        val NORMAL_PHASE_2 = BigAssMaverickRobotTuning(
            attackWait = ATTACK_WAIT_PHASE_2, punchWeight = 2, slamWeight = 2,
            clapWeight = 1, windUpDur = WIND_UP_DUR_PHASE_2, launchMinSpeed = LAUNCH_MIN_SPEED,
            launchMaxSpeed = LAUNCH_MAX_SPEED, lockDur = LOCK_DUR_PHASE_2, restDur = REST_DUR_PHASE_2,
            shockWavePairs = 1, waveLife = WAVE_LIFE_PHASE_2, clapSpeed = CLAP_SPEED,
            orbAims = gdxArrayOf(OrbAim.LEAD, OrbAim.CURRENT, OrbAim.NEAR), orbGap = ORB_GAP_PHASE_2,
            volleyDur = VOLLEY_DUR_PHASE_2, volleyDelay = VOLLEY_DELAY_PHASE_2, orbMinHold = ORB_HOLD_PHASE_2,
            orbMaxHold = ORB_HOLD_PHASE_2, orbMinSpeed = ORB_MIN_SPEED, orbMaxSpeed = ORB_MAX_SPEED,
            orbitSpeedScale = PHASE_2_ORBIT_SPEED_SCALE, hoverTimeScale = PHASE_2_HOVER_TIME_SCALE,
            hoverAmplitudeScale = PHASE_2_HOVER_AMPLITUDE_SCALE, partnerScale = PHASE_2_PARTNER_SCALE,
            schedule = SCHEDULE_PHASE_2
        )
        val HARD = BigAssMaverickRobotTuning(
            attackWait = ATTACK_WAIT_HARD, punchWeight = 2, slamWeight = 1, clapWeight = 0,
            windUpDur = WIND_UP_DUR_HARD, launchMinSpeed = LAUNCH_MIN_SPEED_HARD,
            launchMaxSpeed = LAUNCH_MAX_SPEED_HARD, lockDur = LOCK_DUR_HARD, restDur = REST_DUR_HARD,
            shockWavePairs = 1, waveLife = WAVE_LIFE_HARD, clapSpeed = CLAP_SPEED_HARD,
            orbAims = gdxArrayOf(OrbAim.LEAD, OrbAim.CURRENT, OrbAim.NEAR), orbGap = ORB_GAP_HARD,
            volleyDur = VOLLEY_DUR_HARD, volleyDelay = VOLLEY_DELAY_HARD, orbMinHold = ORB_MIN_HOLD_HARD,
            orbMaxHold = ORB_MAX_HOLD_HARD, orbMinSpeed = ORB_MIN_SPEED_HARD, orbMaxSpeed = ORB_MAX_SPEED_HARD,
            orbitSpeedScale = 1f, hoverTimeScale = 1f, hoverAmplitudeScale = 1f, partnerScale = 1f,
            schedule = SCHEDULE
        )
        val HARD_PHASE_2 = BigAssMaverickRobotTuning(
            attackWait = ATTACK_WAIT_HARD_PHASE_2, punchWeight = 2, slamWeight = 2,
            clapWeight = 2, windUpDur = WIND_UP_DUR_HARD_PHASE_2, launchMinSpeed = LAUNCH_MIN_SPEED_HARD,
            launchMaxSpeed = LAUNCH_MAX_SPEED_HARD, lockDur = LOCK_DUR_HARD_PHASE_2, restDur = REST_DUR_HARD_PHASE_2,
            shockWavePairs = 2, waveLife = WAVE_LIFE_HARD_PHASE_2, clapSpeed = CLAP_SPEED_HARD,
            orbAims = gdxArrayOf(OrbAim.LEAD, OrbAim.CURRENT, OrbAim.NEAR, OrbAim.WIDE),
            orbGap = ORB_GAP_HARD_PHASE_2, volleyDur = VOLLEY_DUR_HARD_PHASE_2,
            volleyDelay = VOLLEY_DELAY_HARD_PHASE_2, orbMinHold = ORB_HOLD_HARD_PHASE_2,
            orbMaxHold = ORB_HOLD_HARD_PHASE_2, orbMinSpeed = ORB_MIN_SPEED_HARD, orbMaxSpeed = ORB_MAX_SPEED_HARD,
            orbitSpeedScale = PHASE_2_ORBIT_SPEED_SCALE, hoverTimeScale = PHASE_2_HOVER_TIME_SCALE,
            hoverAmplitudeScale = PHASE_2_HOVER_AMPLITUDE_SCALE, partnerScale = PHASE_2_PARTNER_SCALE,
            schedule = SCHEDULE_PHASE_2
        )

        fun of(hard: Boolean, phase: Int) = when {
            hard && phase >= 2 -> HARD_PHASE_2
            hard -> HARD
            phase >= 2 -> NORMAL_PHASE_2
            else -> NORMAL
        }
    }
}
