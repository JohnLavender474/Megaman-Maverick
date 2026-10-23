package com.maverick.game.engine.players

import com.maverick.game.engine.entities.IGameEntity

interface PlayerView {

    val slot: PlayerSlot

    val entity: IGameEntity

    val present: Boolean
        get() = entity.spawned
}
