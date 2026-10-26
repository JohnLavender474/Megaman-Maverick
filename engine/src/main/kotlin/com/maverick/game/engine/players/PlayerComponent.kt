package com.maverick.game.engine.players

import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.entities.IGameEntity

class PlayerComponent(val slot: PlayerSlot) : IGameComponent

val IGameEntity.playerSlot: PlayerSlot?
    get() = getComponent(PlayerComponent::class)?.slot

val IGameEntity.isPlayer: Boolean
    get() = hasComponent(PlayerComponent::class)
