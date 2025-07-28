package com.megaman.maverick.game.entities.utils

import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.megaman.maverick.game.entities.contracts.IFreezerEntity

class FreezableEntityHandler(duration: Float) : Updatable, Resettable {

    val timer = Timer(duration).setToEnd()

    fun canBeFrozenBy(damager: IDamager) = damager is IFreezerEntity && !isFrozen()

    fun setFrozen(value: Boolean) {
        if (value) timer.reset() else timer.setToEnd()
    }

    fun isFrozen() = !timer.isFinished()

    fun isJustFinished() = timer.isJustFinished()

    override fun update(delta: Float) = timer.update(delta)

    override fun reset() = timer.reset()
}
