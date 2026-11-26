package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.IGameComponent
import com.megaman.maverick.game.weapons.WeaponWindup

class WeaponWindupComponent : IGameComponent {

    var active: WeaponWindup? = null
        private set

    fun hold(windup: WeaponWindup, delta: Float) {
        if (active !== windup) {
            active?.clear()
            active = windup
        }
        windup.hold(delta)
    }

    fun stop() {
        active?.clear()
        active = null
    }

    override fun reset() = stop()
}
