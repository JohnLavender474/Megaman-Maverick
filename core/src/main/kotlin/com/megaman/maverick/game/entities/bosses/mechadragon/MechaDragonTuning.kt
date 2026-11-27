package com.megaman.maverick.game.entities.bosses.mechadragon

class MechaDragonTuning(
    val breathWindupDur: Float,
    val sweepDur: Float,
    val returnSweep: Boolean,
    val landHangDur: Float,
    val chompTellDur: Float,
    val chargeTellMinDur: Float,
    val chargeTellMaxDur: Float
) {

    companion object {
        const val PANIC_TELL_SCALE = 0.8f
        const val PANIC_HEALTH_RATIO = 0.25f

        val NORMAL = MechaDragonTuning(
            breathWindupDur = 0.75f, sweepDur = 1.2f, returnSweep = false, landHangDur = 0.5f, chompTellDur = 0.45f,
            chargeTellMinDur = 1f, chargeTellMaxDur = 2f
        )
        val HARD = MechaDragonTuning(
            breathWindupDur = 0.6f, sweepDur = 0.9f, returnSweep = true, landHangDur = 0.4f, chompTellDur = 0.36f,
            chargeTellMinDur = 0.8f, chargeTellMaxDur = 1.6f
        )
        val NORMAL_PANIC = panic(NORMAL)
        val HARD_PANIC = panic(HARD)

        fun of(hard: Boolean, panic: Boolean) = when {
            hard && panic -> HARD_PANIC
            hard -> HARD
            panic -> NORMAL_PANIC
            else -> NORMAL
        }

        private fun panic(base: MechaDragonTuning) = MechaDragonTuning(
            breathWindupDur = base.breathWindupDur * PANIC_TELL_SCALE,
            sweepDur = base.sweepDur,
            returnSweep = true,
            landHangDur = base.landHangDur * PANIC_TELL_SCALE,
            chompTellDur = base.chompTellDur * PANIC_TELL_SCALE,
            chargeTellMinDur = base.chargeTellMinDur * PANIC_TELL_SCALE,
            chargeTellMaxDur = base.chargeTellMaxDur * PANIC_TELL_SCALE
        )
    }
}
