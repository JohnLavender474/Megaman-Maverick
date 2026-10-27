package com.maverick.game.engine.damage

import com.maverick.game.engine.entities.IGameEntity

interface IDamageable : IGameEntity {

    val invincible: Boolean

    fun canBeDamagedBy(damager: IDamager): Boolean = true

    fun takeDamageFrom(damager: IDamager): Boolean = false
}
