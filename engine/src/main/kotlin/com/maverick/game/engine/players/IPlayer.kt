package com.maverick.game.engine.players

import com.maverick.game.engine.entities.IGameEntity

interface IPlayer : IGameEntity {

    val slot: PlayerSlot
        get() = playerSlot ?: throw PlayerNotRegisteredException(this)
}
