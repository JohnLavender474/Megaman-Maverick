package com.megaman.maverick.game.entities.contracts

import com.megaman.maverick.game.players.MegaPlayer

interface ItemEntity {

    fun contactWithPlayer(player: MegaPlayer)
}
