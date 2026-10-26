package com.megaman.maverick.game.players

import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.entities.IGameEntity

class PlayerLightComponent(val light: () -> PlayerLight) : IGameComponent

val IGameEntity.lightOrNull: PlayerLight?
    get() = getComponent(PlayerLightComponent::class)?.light?.invoke()
