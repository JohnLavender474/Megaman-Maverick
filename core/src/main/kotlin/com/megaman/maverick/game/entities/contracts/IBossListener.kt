package com.megaman.maverick.game.entities.contracts

import com.maverick.game.engine.entities.contracts.IDestroyable

interface IBossListener : IDestroyable {

    fun onBossDefeated(boss: AbstractBoss) {
        destroy()
    }
}
