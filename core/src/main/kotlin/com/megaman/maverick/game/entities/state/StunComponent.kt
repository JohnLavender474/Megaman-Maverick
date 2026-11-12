package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.IGameComponent

class StunComponent(
    val duration: Float,
    val onSurface: () -> Boolean,
    val onStun: () -> Unit
) : IGameComponent {

    private val timer = Timer(duration).setToEnd()

    val stunned: Boolean
        get() = !timer.isFinished()

    fun start(duration: Float = this.duration) {
        timer.resetDuration(duration)
    }

    fun update(delta: Float, grounded: Boolean) {
        if (grounded) timer.update(delta)
    }

    override fun reset() {
        timer.setToEnd()
    }
}
