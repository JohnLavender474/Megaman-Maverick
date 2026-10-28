package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.entities.IGameEntity

val IGameEntity.mapObjectId: Int?
    get() = getComponent(MapObjectComponent::class)?.id
