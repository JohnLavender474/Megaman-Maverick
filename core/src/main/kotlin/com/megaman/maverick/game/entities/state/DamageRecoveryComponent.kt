package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.IGameComponent

class DamageRecoveryComponent(duration: Float, flashInterval: Float) : IGameComponent {

    private val recoveryTimer = Timer(duration).setToEnd()
    private val flashTimer = Timer(flashInterval)

    var flash = false
        private set

    val recovering: Boolean
        get() = !recoveryTimer.isFinished()

    fun update(delta: Float, damaged: Boolean, damageJustFinished: Boolean) {
        if (damageJustFinished) recoveryTimer.reset()

        if (!damaged && recovering) {
            recoveryTimer.update(delta)
            flashTimer.update(delta)
            if (flashTimer.isFinished()) {
                flashTimer.reset()
                flash = !flash
            }
        }

        if (recoveryTimer.isJustFinished()) flash = false
    }

    override fun reset() {
        recoveryTimer.setToEnd()
        flashTimer.reset()
        flash = false
    }
}
