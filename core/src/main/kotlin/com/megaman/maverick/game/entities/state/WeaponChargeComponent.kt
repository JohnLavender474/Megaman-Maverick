package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.time.TimeMarkedRunnable
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.IGameComponent

class WeaponChargeComponent(
    private val halfTime: Float,
    fullTime: Float,
    onHalf: () -> Unit
) : IGameComponent {

    private val timer = Timer(fullTime, TimeMarkedRunnable(halfTime, onHalf))

    val half: Boolean
        get() = timer.time >= halfTime
    val full: Boolean
        get() = timer.isFinished()

    fun update(delta: Float) = timer.update(delta)

    override fun reset() = timer.reset()
}
