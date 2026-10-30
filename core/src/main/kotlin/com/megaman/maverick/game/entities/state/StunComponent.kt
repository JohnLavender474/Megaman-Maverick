package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.IGameComponent

class StunComponent(
    duration: Float,
    val onSurface: () -> Boolean,
    val onStun: () -> Unit
) : IGameComponent {

    private val timer = Timer(duration).setToEnd()

    val stunned: Boolean
        get() = !timer.isFinished()

    fun start() {
        timer.reset()
    }

    fun update(delta: Float, grounded: Boolean) {
        if (grounded) timer.update(delta)
    }

    override fun reset() {
        timer.setToEnd()
    }
}
