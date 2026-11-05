package com.megaman.maverick.game.fights

import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.screens.levels.stats.BossHealthHandler

class FightDirector {

    val active: Boolean
        get() = fight != null

    private var fight: Fight? = null

    fun start(
        boss: AbstractBoss,
        plan: FightPlan,
        services: MegaServices,
        bar: BossHealthHandler,
        levelMusic: () -> MusicAsset?
    ) {
        fight = Fight(boss, plan, services, bar, levelMusic)
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
