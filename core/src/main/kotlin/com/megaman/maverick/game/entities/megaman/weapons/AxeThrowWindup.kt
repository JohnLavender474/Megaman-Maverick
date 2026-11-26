package com.megaman.maverick.game.entities.megaman.weapons

import com.maverick.game.engine.common.time.Timer
import com.megaman.maverick.game.weapons.WeaponWindup

class AxeThrowWindup(fullTime: Float) : WeaponWindup {

    private val timer = Timer(fullTime)

    val time: Float
        get() = timer.time

    val progress: Float
        get() = timer.getRatio()

    override fun hold(delta: Float) = timer.update(delta)

    override fun clear() = timer.reset()
}
