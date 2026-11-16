package com.megaman.maverick.game.entities.bosses.desertman

class DesertManTuning(
    val standDur: Float,
    val burrowChance: Float,
    val punchChance: Float,
    val tornadoChance: Float,
    val danceChance: Float,
    val windUpDur: Float,
    val punchExtendSpeed: Float,
    val punchHoldDur: Float,
    val throwChance: Float,
    val throwWindUpDur: Float,
    val throwHoldDur: Float,
    val throwVolleys: Int,
    val throwNeedles: Int,
    val throwNeedleSpread: Float,
    val throwNeedleSpeed: Float,
    val tornadoSpinUpDur: Float,
    val tornadoTurns: Int,
    val burrowSinkDur: Float,
    val burrowTrailSpeed: Float,
    val burrowTellDur: Float,
    val eruptImpulseY: Float,
    val needleSpinChance: Float,
    val needleSpinBursts: Int,
    val needleSpinBurstDelay: Float,
    val needleSpinTellDur: Float
) {

    companion object {
        const val STAND_DUR = 1f
        const val STAND_DUR_PHASE_2 = 0.8f
        const val STAND_DUR_HARD = 0.75f
        const val STAND_DUR_HARD_PHASE_2 = 0.6f
        const val BURROW_CHANCE_PHASE_2 = 0.35f
        const val BURROW_CHANCE_HARD_PHASE_2 = 0.4f
        const val PUNCH_CHANCE = 0.75f
        const val TORNADO_CHANCE = 0.6f
        const val TORNADO_CHANCE_HARD = 0.7f
        const val DANCE_CHANCE = 0.5f
        const val WIND_UP_DUR = 0.5f
        const val WIND_UP_DUR_PHASE_2 = 0.4f
        const val WIND_UP_DUR_HARD = 0.35f
        const val PUNCH_EXTEND_SPEED = 24f
        const val PUNCH_EXTEND_SPEED_PHASE_2 = 28f
        const val PUNCH_EXTEND_SPEED_HARD = 28f
        const val PUNCH_EXTEND_SPEED_HARD_PHASE_2 = 32f
        const val PUNCH_HOLD_DUR = 0.35f
        const val PUNCH_HOLD_DUR_PHASE_2 = 0.3f
        const val PUNCH_HOLD_DUR_HARD = 0.3f
        const val PUNCH_HOLD_DUR_HARD_PHASE_2 = 0.25f
        const val THROW_CHANCE = 0.5f
        const val THROW_CHANCE_PHASE_2 = 0.6f
        const val THROW_CHANCE_HARD = 0.6f
        const val THROW_CHANCE_HARD_PHASE_2 = 0.7f
        const val THROW_WIND_UP_DUR = 0.6f
        const val THROW_WIND_UP_DUR_PHASE_2 = 0.5f
        const val THROW_WIND_UP_DUR_HARD = 0.45f
        const val THROW_WIND_UP_DUR_HARD_PHASE_2 = 0.4f
        const val THROW_HOLD_DUR = 0.35f
        const val THROW_HOLD_DUR_PHASE_2 = 0.3f
        const val THROW_HOLD_DUR_HARD = 0.3f
        const val THROW_HOLD_DUR_HARD_PHASE_2 = 0.25f
        const val THROW_NEEDLE_SPREAD = 20f
        const val THROW_NEEDLE_SPREAD_PHASE_2 = 18f
        const val THROW_NEEDLE_SPREAD_HARD_PHASE_2 = 15f
        const val THROW_NEEDLE_SPEED = 10f
        const val THROW_NEEDLE_SPEED_PHASE_2 = 11f
        const val THROW_NEEDLE_SPEED_HARD = 12f
        const val TORNADO_SPIN_UP_DUR = 0.5f
        const val TORNADO_SPIN_UP_DUR_HARD_PHASE_2 = 0.35f
        const val BURROW_SINK_DUR = 0.4f
        const val BURROW_TRAIL_SPEED = 8f
        const val BURROW_TRAIL_SPEED_HARD = 10f
        const val BURROW_TELL_DUR = 0.6f
        const val ERUPT_IMPULSE_Y = 15f
        const val NEEDLE_SPIN_CHANCE = 0.45f
        const val NEEDLE_SPIN_CHANCE_PHASE_2 = 0.5f
        const val NEEDLE_SPIN_CHANCE_HARD = 0.5f
        const val NEEDLE_SPIN_CHANCE_HARD_PHASE_2 = 0.55f
        const val NEEDLE_SPIN_BURST_DELAY = 0.6f
        const val NEEDLE_SPIN_BURST_DELAY_NORMAL_PHASE_2 = 0.5f
        const val NEEDLE_SPIN_BURST_DELAY_PHASE_2 = 0.4f
        const val NEEDLE_SPIN_TELL_DUR = 0.6f
        const val NEEDLE_SPIN_TELL_DUR_PHASE_2 = 0.5f
        const val NEEDLE_SPIN_TELL_DUR_HARD = 0.35f
        const val NEEDLE_SPIN_TELL_DUR_HARD_PHASE_2 = 0.3f
        const val NEEDLE_SPIN_BURST_DELAY_HARD_PHASE_2 = 0.35f

        val NORMAL = DesertManTuning(
            standDur = STAND_DUR, burrowChance = 0f, punchChance = PUNCH_CHANCE, tornadoChance = TORNADO_CHANCE,
            danceChance = DANCE_CHANCE, windUpDur = WIND_UP_DUR,
            punchExtendSpeed = PUNCH_EXTEND_SPEED, punchHoldDur = PUNCH_HOLD_DUR,
            throwChance = THROW_CHANCE, throwWindUpDur = THROW_WIND_UP_DUR, throwHoldDur = THROW_HOLD_DUR,
            throwVolleys = 1, throwNeedles = 5, throwNeedleSpread = THROW_NEEDLE_SPREAD,
            throwNeedleSpeed = THROW_NEEDLE_SPEED,
            tornadoSpinUpDur = TORNADO_SPIN_UP_DUR, tornadoTurns = 0, burrowSinkDur = BURROW_SINK_DUR,
            burrowTrailSpeed = BURROW_TRAIL_SPEED, burrowTellDur = BURROW_TELL_DUR, eruptImpulseY = ERUPT_IMPULSE_Y,
            needleSpinChance = NEEDLE_SPIN_CHANCE, needleSpinBursts = 2,
            needleSpinBurstDelay = NEEDLE_SPIN_BURST_DELAY, needleSpinTellDur = NEEDLE_SPIN_TELL_DUR
        )
        val NORMAL_PHASE_2 = DesertManTuning(
            standDur = STAND_DUR_PHASE_2, burrowChance = BURROW_CHANCE_PHASE_2, punchChance = PUNCH_CHANCE,
            tornadoChance = TORNADO_CHANCE, danceChance = DANCE_CHANCE,
            windUpDur = WIND_UP_DUR_PHASE_2, punchExtendSpeed = PUNCH_EXTEND_SPEED_PHASE_2,
            punchHoldDur = PUNCH_HOLD_DUR_PHASE_2, tornadoSpinUpDur = TORNADO_SPIN_UP_DUR, tornadoTurns = 1,
            throwChance = THROW_CHANCE_PHASE_2, throwWindUpDur = THROW_WIND_UP_DUR_PHASE_2,
            throwHoldDur = THROW_HOLD_DUR_PHASE_2, throwVolleys = 2, throwNeedles = 5,
            throwNeedleSpread = THROW_NEEDLE_SPREAD_PHASE_2, throwNeedleSpeed = THROW_NEEDLE_SPEED_PHASE_2,
            burrowSinkDur = BURROW_SINK_DUR, burrowTrailSpeed = BURROW_TRAIL_SPEED, burrowTellDur = BURROW_TELL_DUR,
            eruptImpulseY = ERUPT_IMPULSE_Y, needleSpinChance = NEEDLE_SPIN_CHANCE_PHASE_2, needleSpinBursts = 2,
            needleSpinBurstDelay = NEEDLE_SPIN_BURST_DELAY_NORMAL_PHASE_2,
            needleSpinTellDur = NEEDLE_SPIN_TELL_DUR_PHASE_2
        )
        val HARD = DesertManTuning(
            standDur = STAND_DUR_HARD, burrowChance = 0f, punchChance = PUNCH_CHANCE,
            tornadoChance = TORNADO_CHANCE_HARD, danceChance = DANCE_CHANCE,
            windUpDur = WIND_UP_DUR_HARD, punchExtendSpeed = PUNCH_EXTEND_SPEED_HARD,
            punchHoldDur = PUNCH_HOLD_DUR_HARD, tornadoSpinUpDur = TORNADO_SPIN_UP_DUR, tornadoTurns = 0,
            throwChance = THROW_CHANCE_HARD, throwWindUpDur = THROW_WIND_UP_DUR_HARD,
            throwHoldDur = THROW_HOLD_DUR_HARD, throwVolleys = 2, throwNeedles = 5,
            throwNeedleSpread = THROW_NEEDLE_SPREAD_PHASE_2, throwNeedleSpeed = THROW_NEEDLE_SPEED_HARD,
            burrowSinkDur = BURROW_SINK_DUR, burrowTrailSpeed = BURROW_TRAIL_SPEED_HARD,
            burrowTellDur = BURROW_TELL_DUR, eruptImpulseY = ERUPT_IMPULSE_Y,
            needleSpinChance = NEEDLE_SPIN_CHANCE_HARD, needleSpinBursts = 3,
            needleSpinBurstDelay = NEEDLE_SPIN_BURST_DELAY_PHASE_2, needleSpinTellDur = NEEDLE_SPIN_TELL_DUR_HARD
        )
        val HARD_PHASE_2 = DesertManTuning(
            standDur = STAND_DUR_HARD_PHASE_2, burrowChance = BURROW_CHANCE_HARD_PHASE_2, punchChance = PUNCH_CHANCE,
            tornadoChance = TORNADO_CHANCE_HARD, danceChance = DANCE_CHANCE,
            windUpDur = WIND_UP_DUR_HARD, punchExtendSpeed = PUNCH_EXTEND_SPEED_HARD_PHASE_2,
            punchHoldDur = PUNCH_HOLD_DUR_HARD_PHASE_2, tornadoSpinUpDur = TORNADO_SPIN_UP_DUR_HARD_PHASE_2,
            throwChance = THROW_CHANCE_HARD_PHASE_2, throwWindUpDur = THROW_WIND_UP_DUR_HARD_PHASE_2,
            throwHoldDur = THROW_HOLD_DUR_HARD_PHASE_2, throwVolleys = 3, throwNeedles = 7,
            throwNeedleSpread = THROW_NEEDLE_SPREAD_HARD_PHASE_2, throwNeedleSpeed = THROW_NEEDLE_SPEED_HARD,
            tornadoTurns = 1,
            burrowSinkDur = BURROW_SINK_DUR, burrowTrailSpeed = BURROW_TRAIL_SPEED_HARD,
            burrowTellDur = BURROW_TELL_DUR, eruptImpulseY = ERUPT_IMPULSE_Y,
            needleSpinChance = NEEDLE_SPIN_CHANCE_HARD_PHASE_2, needleSpinBursts = 4,
            needleSpinBurstDelay = NEEDLE_SPIN_BURST_DELAY_HARD_PHASE_2,
            needleSpinTellDur = NEEDLE_SPIN_TELL_DUR_HARD_PHASE_2
        )

        fun of(hard: Boolean, phase: Int) = when {
            hard && phase >= 2 -> HARD_PHASE_2
            hard -> HARD
            phase >= 2 -> NORMAL_PHASE_2
            else -> NORMAL
        }
    }
}
