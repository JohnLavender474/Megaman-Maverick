package com.megaman.maverick.game.players

import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.IGameComponent

class PlayerStateComponent(private val damageTimer: Timer, private val shootAnimTimer: Timer) : IGameComponent {

    var ready = false

    var canBeDamaged = true

    val damaged: Boolean
        get() = !damageTimer.isFinished()

    val shooting: Boolean
        get() = !shootAnimTimer.isFinished()

    val damageListeners = OrderedSet<IPlayerDamageListener>()
}
