package com.megaman.maverick.game.entities.contracts

import com.maverick.game.engine.players.IPlayer

interface ItemEntity : IMegaGameEntity {

    fun contactWithPlayer(player: IPlayer)
}
