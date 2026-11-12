package com.megaman.maverick.game.entities.bosses.d1000

import com.megaman.maverick.game.ConstVals

// TODO: the D-1000 fight (its behaviors, mechanics, tuning and anti-trap rules) was AI-generated from owner play tests; it needs more scrutiny and refinement (see the branch NOTES of 081f, section "AI-generated fight")
class D1000Tuning(
    val tier: D1000Tier,
    val hard: Boolean,
    val xMoves: D1000Moves,
    val thinkDur: Float,
    val walkSpeed: Float,
    val ramWindUpDur: Float,
    val ramMaxSpeed: Float,
    val ricochetBounces: Int,
    val wallCrashPieces: Int,
    val xStunDur: Float,
    val crouchDebris: FloatArray,
    val slamShockwaves: Boolean,
    val ceilingJitterDur: Float,
    val ceilingLanes: Int,
    val leadTime: Float,
    val leadMax: Float,
    val bombCount: Int,
    val bracketCount: Int,
    val sweepCount: Int,
    val potShotInterval: Float,
    val potTellDur: Float,
    val burstTellDur: Float,
    val spreadSecondFan: Boolean,
    val guideSignalDur: Float,
    val diveTellDur: Float,
    val flankPause: Float,
    val flankShots: Int,
    val dodgeCooldown: Float,
    val dodgeChance: Float
) {

    companion object {
        const val THINK_DUR = 0.8f
        const val THINK_DUR_HARD = 0.5f
        const val THINK_DUR_OVERDRIVE = 0.55f
        const val THINK_DUR_OVERDRIVE_HARD = 0.35f
        const val WALK_SPEED = 3.5f * ConstVals.PPM
        const val WALK_SPEED_OVERDRIVE = 4.25f * ConstVals.PPM
        const val RAM_WIND_UP_DUR = 1.8f
        const val RAM_WIND_UP_DUR_OVERDRIVE = 1.3f
        const val RAM_MAX_SPEED = 9f * ConstVals.PPM
        const val RAM_MAX_SPEED_HARD = 10.5f * ConstVals.PPM
        const val RAM_MAX_SPEED_OVERDRIVE = 10.5f * ConstVals.PPM
        const val RAM_MAX_SPEED_OVERDRIVE_HARD = 11.75f * ConstVals.PPM
        const val RICOCHET_BOUNCES = 1
        const val WALL_CRASH_PIECES = 5
        const val WALL_CRASH_PIECES_HARD = 6
        const val X_STUN_DUR = 0.5f
        const val X_STUN_DUR_HARD = 0.75f
        val CROUCH_DEBRIS = floatArrayOf(-2.5f, 0f, 2.5f)
        val CROUCH_DEBRIS_HARD = floatArrayOf(-3.5f, -1.75f, 0f, 1.75f, 3.5f)
        const val CEILING_JITTER_DUR = 0.5f
        const val CEILING_JITTER_DUR_HARD = 0.3f
        const val CEILING_LANES = 2
        const val CEILING_LANES_OVERDRIVE = 3

        const val LEAD_TIME = 0.3f
        const val LEAD_TIME_HARD = 0.4f
        const val LEAD_MAX = 1.5f * ConstVals.PPM
        const val LEAD_MAX_HARD = 1.5f * ConstVals.PPM
        const val BOMB_COUNT = 1
        const val BOMB_COUNT_HARD = 2
        const val BRACKET_COUNT = 3
        const val BRACKET_COUNT_HARD = 4
        const val SWEEP_COUNT = 3
        const val SWEEP_COUNT_HARD = 4

        const val POT_SHOT_INTERVAL = 2f
        const val POT_SHOT_INTERVAL_HARD = 1.4f
        const val POT_TELL_DUR = 0.3f
        const val POT_TELL_DUR_HARD = 0.2f
        const val BURST_TELL_DUR = 0.3f
        const val BURST_TELL_DUR_HARD = 0.2f
        const val GUIDE_SIGNAL_DUR = 0.3f
        const val GUIDE_SIGNAL_DUR_HARD = 0.25f
        const val GUIDE_SIGNAL_DUR_OVERDRIVE = 0.2f
        const val DIVE_TELL_DUR = 0.6f
        const val DIVE_TELL_DUR_HARD = 0.4f
        const val FLANK_PAUSE = 0.8f
        const val FLANK_PAUSE_HARD = 0.6f
        const val FLANK_SHOTS = 2
        const val FLANK_SHOTS_HARD = 3
        const val DODGE_COOLDOWN = 1.5f
        const val DODGE_COOLDOWN_HARD = 1.1f
        const val DODGE_CHANCE = 0.5f
        const val DODGE_CHANCE_HARD = 0.65f

        val NORMAL = D1000Tuning(
            tier = D1000Tier.NORMAL,
            hard = false,
            xMoves = D1000Moves.NORMAL,
            thinkDur = THINK_DUR,
            walkSpeed = WALK_SPEED,
            ramWindUpDur = RAM_WIND_UP_DUR,
            ramMaxSpeed = RAM_MAX_SPEED,
            ricochetBounces = 0,
            wallCrashPieces = WALL_CRASH_PIECES,
            xStunDur = X_STUN_DUR,
            crouchDebris = CROUCH_DEBRIS,
            slamShockwaves = false,
            ceilingJitterDur = CEILING_JITTER_DUR,
            ceilingLanes = CEILING_LANES,
            leadTime = LEAD_TIME,
            leadMax = LEAD_MAX,
            bombCount = BOMB_COUNT,
            bracketCount = BRACKET_COUNT,
            sweepCount = SWEEP_COUNT,
            potShotInterval = POT_SHOT_INTERVAL,
            potTellDur = POT_TELL_DUR,
            burstTellDur = BURST_TELL_DUR,
            spreadSecondFan = false,
            guideSignalDur = GUIDE_SIGNAL_DUR,
            diveTellDur = DIVE_TELL_DUR,
            flankPause = FLANK_PAUSE,
            flankShots = FLANK_SHOTS,
            dodgeCooldown = DODGE_COOLDOWN,
            dodgeChance = DODGE_CHANCE
        )

        val HARD = D1000Tuning(
            tier = D1000Tier.NORMAL,
            hard = true,
            xMoves = D1000Moves.HARD,
            thinkDur = THINK_DUR_HARD,
            walkSpeed = WALK_SPEED,
            ramWindUpDur = RAM_WIND_UP_DUR,
            ramMaxSpeed = RAM_MAX_SPEED_HARD,
            ricochetBounces = 0,
            wallCrashPieces = WALL_CRASH_PIECES_HARD,
            xStunDur = X_STUN_DUR_HARD,
            crouchDebris = CROUCH_DEBRIS_HARD,
            slamShockwaves = false,
            ceilingJitterDur = CEILING_JITTER_DUR_HARD,
            ceilingLanes = CEILING_LANES,
            leadTime = LEAD_TIME_HARD,
            leadMax = LEAD_MAX_HARD,
            bombCount = BOMB_COUNT_HARD,
            bracketCount = BRACKET_COUNT_HARD,
            sweepCount = SWEEP_COUNT_HARD,
            potShotInterval = POT_SHOT_INTERVAL_HARD,
            potTellDur = POT_TELL_DUR_HARD,
            burstTellDur = BURST_TELL_DUR_HARD,
            spreadSecondFan = false,
            guideSignalDur = GUIDE_SIGNAL_DUR_HARD,
            diveTellDur = DIVE_TELL_DUR_HARD,
            flankPause = FLANK_PAUSE_HARD,
            flankShots = FLANK_SHOTS_HARD,
            dodgeCooldown = DODGE_COOLDOWN_HARD,
            dodgeChance = DODGE_CHANCE_HARD
        )

        val OVERDRIVE = D1000Tuning(
            tier = D1000Tier.OVERDRIVE,
            hard = false,
            xMoves = D1000Moves.NORMAL,
            thinkDur = THINK_DUR_OVERDRIVE,
            walkSpeed = WALK_SPEED_OVERDRIVE,
            ramWindUpDur = RAM_WIND_UP_DUR_OVERDRIVE,
            ramMaxSpeed = RAM_MAX_SPEED_OVERDRIVE,
            ricochetBounces = RICOCHET_BOUNCES,
            wallCrashPieces = WALL_CRASH_PIECES,
            xStunDur = X_STUN_DUR,
            crouchDebris = CROUCH_DEBRIS,
            slamShockwaves = true,
            ceilingJitterDur = CEILING_JITTER_DUR,
            ceilingLanes = CEILING_LANES_OVERDRIVE,
            leadTime = LEAD_TIME,
            leadMax = LEAD_MAX,
            bombCount = BOMB_COUNT,
            bracketCount = BRACKET_COUNT,
            sweepCount = SWEEP_COUNT,
            potShotInterval = POT_SHOT_INTERVAL,
            potTellDur = POT_TELL_DUR,
            burstTellDur = BURST_TELL_DUR,
            spreadSecondFan = true,
            guideSignalDur = GUIDE_SIGNAL_DUR_OVERDRIVE,
            diveTellDur = DIVE_TELL_DUR,
            flankPause = FLANK_PAUSE,
            flankShots = FLANK_SHOTS,
            dodgeCooldown = DODGE_COOLDOWN,
            dodgeChance = DODGE_CHANCE
        )

        val OVERDRIVE_HARD = D1000Tuning(
            tier = D1000Tier.OVERDRIVE,
            hard = true,
            xMoves = D1000Moves.HARD,
            thinkDur = THINK_DUR_OVERDRIVE_HARD,
            walkSpeed = WALK_SPEED_OVERDRIVE,
            ramWindUpDur = RAM_WIND_UP_DUR_OVERDRIVE,
            ramMaxSpeed = RAM_MAX_SPEED_OVERDRIVE_HARD,
            ricochetBounces = RICOCHET_BOUNCES,
            wallCrashPieces = WALL_CRASH_PIECES_HARD,
            xStunDur = X_STUN_DUR_HARD,
            crouchDebris = CROUCH_DEBRIS_HARD,
            slamShockwaves = true,
            ceilingJitterDur = CEILING_JITTER_DUR_HARD,
            ceilingLanes = CEILING_LANES_OVERDRIVE,
            leadTime = LEAD_TIME_HARD,
            leadMax = LEAD_MAX_HARD,
            bombCount = BOMB_COUNT_HARD,
            bracketCount = BRACKET_COUNT_HARD,
            sweepCount = SWEEP_COUNT_HARD,
            potShotInterval = POT_SHOT_INTERVAL_HARD,
            potTellDur = POT_TELL_DUR_HARD,
            burstTellDur = BURST_TELL_DUR_HARD,
            spreadSecondFan = true,
            guideSignalDur = GUIDE_SIGNAL_DUR_OVERDRIVE,
            diveTellDur = DIVE_TELL_DUR_HARD,
            flankPause = FLANK_PAUSE_HARD,
            flankShots = FLANK_SHOTS_HARD,
            dodgeCooldown = DODGE_COOLDOWN_HARD,
            dodgeChance = DODGE_CHANCE_HARD
        )

        fun of(tier: D1000Tier, hard: Boolean) = when (tier) {
            D1000Tier.NORMAL -> if (hard) HARD else NORMAL
            D1000Tier.OVERDRIVE -> if (hard) OVERDRIVE_HARD else OVERDRIVE
        }
    }
}
