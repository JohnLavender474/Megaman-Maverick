package com.megaman.maverick.game.players

import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.IGameComponent

class PlayerStateComponent(private val damageTimer: Timer) : IGameComponent {

    var ready = false

    val damaged: Boolean
        get() = !damageTimer.isFinished()
}
