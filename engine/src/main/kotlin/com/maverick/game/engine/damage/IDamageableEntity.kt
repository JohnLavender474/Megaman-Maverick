package com.maverick.game.engine.damage

import com.maverick.game.engine.entities.IGameEntity

interface IDamageableEntity : IGameEntity {

    val invincible: Boolean

    fun canBeDamagedBy(damager: IDamagerEntity): Boolean = true

    fun takeDamageFrom(damager: IDamagerEntity): Boolean = false
}
