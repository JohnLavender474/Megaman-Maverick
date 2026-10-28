package com.maverick.game.engine.damage

import com.maverick.game.engine.entities.IGameEntity

interface IDamagerEntity : IGameEntity {

    fun canDamage(damageable: IDamageableEntity): Boolean = true

    fun onDamageInflictedTo(damageable: IDamageableEntity) {}
}
