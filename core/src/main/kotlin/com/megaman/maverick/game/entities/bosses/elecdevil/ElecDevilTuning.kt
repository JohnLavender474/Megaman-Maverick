package com.megaman.maverick.game.entities.bosses.elecdevil

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.megaman.maverick.game.entities.bosses.ElecDevilState
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.OrbAim

class ElecDevilTuning(
    val appearDur: Float,
    val standDur: Float,
    val chargeDur: Float,
    val turnToPiecesDur: Float,
    val launchDelayMin: Float,
    val launchDelayMax: Float,
    val randomLaunchDelayMin: Float,
    val randomLaunchDelayMax: Float,
    val pieceMinSpeed: Float,
    val pieceMaxSpeed: Float,
    val eyePatterns: Array<EyePattern>,
    val eyeSpreadAims: Array<OrbAim>,
    val eyeSpreadLeadTime: Float,
    val liveWireLinks: Int,
    val liveWireMinGap: Int,
    val clawDelayDur: Float,
    val clawWindupDur: Float,
    val clawSpeed: Float,
    val clawHoldDur: Float
) {

    init {
        if (liveWireMinGap < 1) throw IllegalArgumentException(
            "liveWireMinGap=$liveWireMinGap: a Live Wire link needs at least 1 unlinked pair on each side, " +
                "or the links join into a wall"
        )
    }

    fun durationOf(state: ElecDevilState) = when (state) {
        ElecDevilState.APPEAR -> appearDur
        ElecDevilState.STAND -> standDur
        ElecDevilState.CHARGE -> chargeDur
        ElecDevilState.TURN_TO_PIECES -> turnToPiecesDur
        ElecDevilState.LAUNCH -> throw IllegalArgumentException("LAUNCH has no duration; it ends with its last piece")
        ElecDevilState.HAND -> throw IllegalArgumentException("HAND has no duration; it ends when the claw is back")
    }

    companion object {
        const val APPEAR_DUR = 0.5f
        const val APPEAR_DUR_PHASE_2 = 0.4f
        const val STAND_DUR = 0.5f
        const val STAND_DUR_PHASE_2 = 0.4f
        const val STAND_DUR_HARD_PHASE_2 = 0.35f
        const val CHARGE_DUR = 1f
        const val CHARGE_DUR_PHASE_2 = 0.8f
        const val CHARGE_DUR_HARD_PHASE_2 = 0.7f
        const val TURN_TO_PIECES_DUR = 1f
        const val TURN_TO_PIECES_DUR_PHASE_2 = 0.8f
        const val LAUNCH_DELAY_MIN = 0.45f
        const val LAUNCH_DELAY_MAX = 0.6f
        const val LAUNCH_DELAY_MIN_PHASE_2 = 0.4f
        const val LAUNCH_DELAY_MAX_PHASE_2 = 0.5f
        const val LAUNCH_DELAY_MIN_HARD = 0.375f
        const val LAUNCH_DELAY_MAX_HARD = 0.5f
        const val LAUNCH_DELAY_MIN_HARD_PHASE_2 = 0.325f
        const val LAUNCH_DELAY_MAX_HARD_PHASE_2 = 0.425f
        const val RANDOM_LAUNCH_DELAY_MIN = 0.55f
        const val RANDOM_LAUNCH_DELAY_MAX = 0.7f
        const val RANDOM_LAUNCH_DELAY_MIN_PHASE_2 = 0.5f
        const val RANDOM_LAUNCH_DELAY_MAX_PHASE_2 = 0.6f
        const val RANDOM_LAUNCH_DELAY_MIN_HARD = 0.475f
        const val RANDOM_LAUNCH_DELAY_MAX_HARD = 0.6f
        const val RANDOM_LAUNCH_DELAY_MIN_HARD_PHASE_2 = 0.425f
        const val RANDOM_LAUNCH_DELAY_MAX_HARD_PHASE_2 = 0.525f
        const val PIECE_MIN_SPEED = 10f
        const val PIECE_MAX_SPEED = 12f
        const val PIECE_MIN_SPEED_PHASE_2 = 11f
        const val PIECE_MAX_SPEED_PHASE_2 = 13f
        const val PIECE_MIN_SPEED_HARD_PHASE_2 = 12f
        const val PIECE_MAX_SPEED_HARD_PHASE_2 = 14f
        const val EYE_SPREAD_LEAD_TIME = 0.75f
        const val EYE_SPREAD_LEAD_TIME_PHASE_2 = 1f
        const val EYE_SPREAD_LEAD_TIME_HARD_PHASE_2 = 1.25f
        const val CLAW_DELAY_DUR = 0.2f
        const val CLAW_WINDUP_DUR = 1.4f
        const val CLAW_WINDUP_DUR_PHASE_2 = 1.3f
        const val CLAW_WINDUP_DUR_HARD = 1f
        const val CLAW_WINDUP_DUR_HARD_PHASE_2 = 0.9f
        const val CLAW_SPEED = 20f
        const val CLAW_SPEED_PHASE_2 = 22f
        const val CLAW_HOLD_DUR = 0.5f
        const val CLAW_HOLD_DUR_PHASE_2 = 0.4f

        private val PHASE_1_PATTERNS = gdxArrayOf(EyePattern.FAN, EyePattern.RING, EyePattern.STALL)
        private val PHASE_2_PATTERNS = gdxArrayOf(EyePattern.FAN, EyePattern.RING, EyePattern.STALL, EyePattern.BOUNCE)

        val NORMAL = ElecDevilTuning(
            appearDur = APPEAR_DUR, standDur = STAND_DUR, chargeDur = CHARGE_DUR,
            turnToPiecesDur = TURN_TO_PIECES_DUR,
            launchDelayMin = LAUNCH_DELAY_MIN, launchDelayMax = LAUNCH_DELAY_MAX,
            randomLaunchDelayMin = RANDOM_LAUNCH_DELAY_MIN, randomLaunchDelayMax = RANDOM_LAUNCH_DELAY_MAX,
            pieceMinSpeed = PIECE_MIN_SPEED, pieceMaxSpeed = PIECE_MAX_SPEED,
            eyeSpreadAims = gdxArrayOf(OrbAim.LEAD, OrbAim.CURRENT, OrbAim.NEAR),
            eyeSpreadLeadTime = EYE_SPREAD_LEAD_TIME, liveWireLinks = 3, liveWireMinGap = 3,
            eyePatterns = PHASE_1_PATTERNS,
            clawDelayDur = CLAW_DELAY_DUR,
            clawWindupDur = CLAW_WINDUP_DUR, clawSpeed = CLAW_SPEED, clawHoldDur = CLAW_HOLD_DUR
        )
        val NORMAL_PHASE_2 = ElecDevilTuning(
            appearDur = APPEAR_DUR_PHASE_2, standDur = STAND_DUR_PHASE_2, chargeDur = CHARGE_DUR_PHASE_2,
            turnToPiecesDur = TURN_TO_PIECES_DUR_PHASE_2,
            launchDelayMin = LAUNCH_DELAY_MIN_PHASE_2, launchDelayMax = LAUNCH_DELAY_MAX_PHASE_2,
            randomLaunchDelayMin = RANDOM_LAUNCH_DELAY_MIN_PHASE_2,
            randomLaunchDelayMax = RANDOM_LAUNCH_DELAY_MAX_PHASE_2,
            pieceMinSpeed = PIECE_MIN_SPEED_PHASE_2, pieceMaxSpeed = PIECE_MAX_SPEED_PHASE_2,
            eyeSpreadAims = gdxArrayOf(OrbAim.LEAD, OrbAim.CURRENT, OrbAim.NEAR),
            eyeSpreadLeadTime = EYE_SPREAD_LEAD_TIME_PHASE_2, liveWireLinks = 6, liveWireMinGap = 1,
            eyePatterns = PHASE_2_PATTERNS,
            clawDelayDur = CLAW_DELAY_DUR, clawWindupDur = CLAW_WINDUP_DUR_PHASE_2, clawSpeed = CLAW_SPEED_PHASE_2,
            clawHoldDur = CLAW_HOLD_DUR_PHASE_2
        )
        val HARD = ElecDevilTuning(
            appearDur = APPEAR_DUR, standDur = STAND_DUR, chargeDur = CHARGE_DUR,
            turnToPiecesDur = TURN_TO_PIECES_DUR,
            launchDelayMin = LAUNCH_DELAY_MIN_HARD, launchDelayMax = LAUNCH_DELAY_MAX_HARD,
            randomLaunchDelayMin = RANDOM_LAUNCH_DELAY_MIN_HARD, randomLaunchDelayMax = RANDOM_LAUNCH_DELAY_MAX_HARD,
            pieceMinSpeed = PIECE_MIN_SPEED, pieceMaxSpeed = PIECE_MAX_SPEED,
            eyeSpreadAims = gdxArrayOf(OrbAim.LEAD, OrbAim.CURRENT, OrbAim.NEAR),
            eyeSpreadLeadTime = EYE_SPREAD_LEAD_TIME_PHASE_2, liveWireLinks = 4, liveWireMinGap = 2,
            eyePatterns = PHASE_1_PATTERNS,
            clawDelayDur = 0f,
            clawWindupDur = CLAW_WINDUP_DUR_HARD, clawSpeed = CLAW_SPEED, clawHoldDur = CLAW_HOLD_DUR
        )
        val HARD_PHASE_2 = ElecDevilTuning(
            appearDur = APPEAR_DUR_PHASE_2, standDur = STAND_DUR_HARD_PHASE_2, chargeDur = CHARGE_DUR_HARD_PHASE_2,
            turnToPiecesDur = TURN_TO_PIECES_DUR_PHASE_2,
            launchDelayMin = LAUNCH_DELAY_MIN_HARD_PHASE_2, launchDelayMax = LAUNCH_DELAY_MAX_HARD_PHASE_2,
            randomLaunchDelayMin = RANDOM_LAUNCH_DELAY_MIN_HARD_PHASE_2,
            randomLaunchDelayMax = RANDOM_LAUNCH_DELAY_MAX_HARD_PHASE_2,
            pieceMinSpeed = PIECE_MIN_SPEED_HARD_PHASE_2, pieceMaxSpeed = PIECE_MAX_SPEED_HARD_PHASE_2,
            eyeSpreadAims = gdxArrayOf(OrbAim.LEAD, OrbAim.CURRENT, OrbAim.NEAR),
            eyeSpreadLeadTime = EYE_SPREAD_LEAD_TIME_HARD_PHASE_2, liveWireLinks = 7, liveWireMinGap = 1,
            eyePatterns = PHASE_2_PATTERNS,
            clawDelayDur = 0f, clawWindupDur = CLAW_WINDUP_DUR_HARD_PHASE_2,
            clawSpeed = CLAW_SPEED_PHASE_2, clawHoldDur = CLAW_HOLD_DUR_PHASE_2
        )

        fun of(hard: Boolean, phase: Int) = when {
            hard && phase >= 2 -> HARD_PHASE_2
            hard -> HARD
            phase >= 2 -> NORMAL_PHASE_2
            else -> NORMAL
        }
    }
}
