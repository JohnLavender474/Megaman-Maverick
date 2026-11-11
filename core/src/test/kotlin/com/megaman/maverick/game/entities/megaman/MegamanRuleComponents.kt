package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.locks.LocksComponent
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.locks.MegaLockGroup

fun Megaman.addRuleComponents() {
    if (!hasComponent(LocksComponent::class)) addComponent(LocksComponent(MegaLockGroup.entries.size))
    if (!hasComponent(PlayPhaseComponent::class)) addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
    addComponent(damageRecovery)
    addComponent(defineStunComponent())
    addComponent(defineFreezeComponent())
}
