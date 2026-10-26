package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.IGameEntity

val IGameEntity.playPhase: PlayPhase
    get() = getComponent(PlayPhaseComponent::class)?.phase ?: PlayPhase.IN_PLAY

val IGameEntity.inPlay: Boolean
    get() = playPhase == PlayPhase.IN_PLAY

fun IGameEntity.setPlayPhase(phase: PlayPhase) {
    require(PlayPhaseComponent::class).phase = phase
}
