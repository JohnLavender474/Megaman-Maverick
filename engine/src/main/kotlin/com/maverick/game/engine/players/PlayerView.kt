package com.maverick.game.engine.players

import com.maverick.game.engine.components.ComponentKey
import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.components.get
import com.maverick.game.engine.entities.IGameEntity

interface PlayerView {

    val slot: PlayerSlot

    val entity: IGameEntity

    val present: Boolean
        get() = entity.spawned
}

operator fun <C : IGameComponent> PlayerView.get(key: ComponentKey<C>): C? = entity[key]
