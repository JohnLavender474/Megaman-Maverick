package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.IGameComponent

class PostActionMomentumComponent(
    duration: Float,
    val resistance: Float,
    val maxSpeed: () -> Float,
    val overGround: () -> Boolean,
    val facingBlock: () -> Boolean,
    val groundSliding: () -> Boolean
) : IGameComponent {

    val timer = Timer(duration).setToEnd()
    var pendingKill = false

    override fun reset() {
        timer.setToEnd()
        pendingKill = false
    }
}
