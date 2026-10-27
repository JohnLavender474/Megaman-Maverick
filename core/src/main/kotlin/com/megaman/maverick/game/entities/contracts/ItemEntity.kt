package com.megaman.maverick.game.entities.contracts


interface ItemEntity : IMegaGameEntity {

    fun contactWithPlayer(player: MegaGameEntity)
}
