package com.megaman.maverick.game.weapons

import com.maverick.game.engine.common.time.TimeMarkedRunnable
import com.maverick.game.engine.common.time.Timer
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus

class BusterCharge(
    private val fullTime: Float,
    private val halfTime: Float,
    private val speedScalar: () -> Float,
    onHalfCharged: () -> Unit,
    onFullyCharged: () -> Unit,
    private val onCleared: () -> Unit
) : WeaponWindup {

    private val timer = Timer(
        fullTime,
        TimeMarkedRunnable(halfTime, onHalfCharged),
        TimeMarkedRunnable(fullTime, onFullyCharged)
    )

    override fun hold(delta: Float) = timer.update(delta * speedScalar())

    override fun clear() {
        timer.reset()
        onCleared()
    }

    fun status() = when {
        timer.time >= fullTime -> MegaChargeStatus.FULLY_CHARGED
        timer.time >= halfTime -> MegaChargeStatus.HALF_CHARGED
        else -> MegaChargeStatus.NOT_CHARGED
    }
}
