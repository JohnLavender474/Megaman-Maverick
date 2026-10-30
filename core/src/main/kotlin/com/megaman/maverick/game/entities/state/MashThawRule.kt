package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.time.Timer

class MashThawRule(
    private val mashesToThaw: Int,
    mashCooldown: Float,
    private val mashed: () -> Boolean,
    private val onMash: () -> Unit
) : IThawRule {

    private var mashes = 0
    private val cooldown = Timer(mashCooldown).setToEnd()

    override fun shouldThaw(delta: Float): Boolean {
        cooldown.update(delta)
        if (cooldown.isFinished() && mashed()) {
            mashes++
            cooldown.reset()
            onMash()
        }
        return mashes >= mashesToThaw
    }

    override fun reset() {
        mashes = 0
        cooldown.setToEnd()
    }
}
