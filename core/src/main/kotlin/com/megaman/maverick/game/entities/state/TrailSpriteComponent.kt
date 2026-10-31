package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.IGameComponent

class TrailSpriteComponent(delay: Float, val trySpawn: () -> Boolean) : IGameComponent {

    val timer = Timer(delay)

    fun update(delta: Float) {
        timer.update(delta)
        if (timer.isFinished() && trySpawn()) timer.reset()
    }
}
