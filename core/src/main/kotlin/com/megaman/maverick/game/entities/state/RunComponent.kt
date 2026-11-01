package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.IGameComponent

class RunComponent : IGameComponent {

    var running = false
    var runTime = 0f

    override fun reset() {
        running = false
        runTime = 0f
    }
}
