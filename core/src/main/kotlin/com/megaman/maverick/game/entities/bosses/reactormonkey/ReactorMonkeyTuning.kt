package com.megaman.maverick.game.entities.bosses.reactormonkey

class ReactorMonkeyTuning(
    val throwDelayMin: Float,
    val throwDelayMax: Float,
    val fakeHoldMin: Float,
    val fakeHoldMax: Float,
    val leadTime: Float,
    val skipBounces: Int
) {

    companion object {

        val NORMAL = ReactorMonkeyTuning(
            throwDelayMin = 2f, throwDelayMax = 2.5f, fakeHoldMin = 0.3f, fakeHoldMax = 0.6f, leadTime = 0.8f,
            skipBounces = 4
        )
        val NORMAL_PHASE_2 = ReactorMonkeyTuning(
            throwDelayMin = 1.3f, throwDelayMax = 1.8f, fakeHoldMin = 0.3f, fakeHoldMax = 0.5f, leadTime = 0.8f,
            skipBounces = 5
        )
        val HARD = ReactorMonkeyTuning(
            throwDelayMin = 1.2f, throwDelayMax = 1.5f, fakeHoldMin = 0.2f, fakeHoldMax = 0.5f, leadTime = 1f,
            skipBounces = 4
        )
        val HARD_PHASE_2 = ReactorMonkeyTuning(
            throwDelayMin = 0.8f, throwDelayMax = 1.1f, fakeHoldMin = 0.2f, fakeHoldMax = 0.4f, leadTime = 1f,
            skipBounces = 5
        )

        fun of(hard: Boolean, phase: Int) = when {
            hard && phase >= 2 -> HARD_PHASE_2
            hard -> HARD
            phase >= 2 -> NORMAL_PHASE_2
            else -> NORMAL
        }
    }
}
