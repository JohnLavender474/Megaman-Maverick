package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.entities.contracts.IDestroyable

interface IBossListener : IDestroyable {

    fun onBossDefeated(boss: AbstractBoss) {
        destroy()
    }
}
