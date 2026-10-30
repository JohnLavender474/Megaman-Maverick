package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.require
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.IGameEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity

val IGameEntity.frozen: Boolean
    get() = getComponent(FreezeComponent::class)?.frozen == true

fun IGameEntity.freezeFrom(damager: IDamagerEntity) {
    if (damager is IFreezerEntity) require(FreezeComponent::class).freeze()
}
