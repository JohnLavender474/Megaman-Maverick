package com.maverick.game.engine.damage

import com.maverick.game.engine.entities.IGameEntity

interface IDamager : IGameEntity {

    fun canDamage(damageable: IDamageable): Boolean = true

    fun onDamageInflictedTo(damageable: IDamageable) {}
}
