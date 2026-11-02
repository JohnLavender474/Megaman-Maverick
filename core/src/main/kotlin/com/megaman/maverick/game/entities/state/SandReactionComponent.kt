package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.entities.IGameEntity

enum class SandReaction { SINK, SUBMERGE, SURFACE }

class SandReactionComponent(val reaction: SandReaction) : IGameComponent

val IGameEntity.sandReaction: SandReaction
    get() = getComponent(SandReactionComponent::class)?.reaction ?: SandReaction.SURFACE
