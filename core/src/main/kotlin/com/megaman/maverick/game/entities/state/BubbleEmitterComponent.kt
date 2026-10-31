package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.IGameComponent

class BubbleEmitterComponent(delay: Float, val canEmit: () -> Boolean, val emit: () -> Unit) : IGameComponent {

    val timer = Timer(delay)

    fun update(delta: Float) {
        if (!canEmit()) return

        timer.update(delta)
        if (timer.isFinished()) {
            emit()
            timer.reset()
        }
    }
}
