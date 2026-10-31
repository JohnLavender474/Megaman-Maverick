package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.IGameComponent

class RoomTransitionCarryComponent(pauseDuration: Float, val carries: () -> Boolean) : IGameComponent {

    val pauseTimer = Timer(pauseDuration).setToEnd()
    var carriedX: Float? = null
    var carriedY: Float? = null

    fun update(delta: Float) {
        pauseTimer.update(delta)
    }

    override fun reset() {
        pauseTimer.setToEnd()
    }
}
