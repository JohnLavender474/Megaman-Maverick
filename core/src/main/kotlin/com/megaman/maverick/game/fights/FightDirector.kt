package com.megaman.maverick.game.fights

import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.screens.levels.stats.BossHealthHandler

class FightDirector {

    val active: Boolean
        get() = fight != null

    private var fight: Fight? = null

    fun owns(boss: AbstractBoss) = fight?.boss === boss

    fun start(boss: AbstractBoss, plan: FightPlan, services: MegaServices, bar: BossHealthHandler) {
        fight = Fight(boss, plan, services, bar)
    }

    fun update() {
        val fight = fight ?: return
        fight.update()
        if (fight.closed) this.fight = null
    }

    fun cancel() {
        fight?.cancel()
        fight = null
    }
}
