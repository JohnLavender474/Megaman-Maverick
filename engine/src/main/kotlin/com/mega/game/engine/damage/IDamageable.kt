package com.mega.game.engine.damage

interface IDamageable {

    val invincible: Boolean

    fun canBeDamagedBy(damager: IDamager): Boolean = true

    fun takeDamageFrom(damager: IDamager): Boolean = false
}
