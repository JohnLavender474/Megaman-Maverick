package com.megaman.maverick.game.entities.contracts

interface IBossListener : IMegaGameEntity {

    fun onBossDefeated(boss: AbstractBoss) {
        destroy()
    }
}
