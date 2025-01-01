package com.mega.game.engine.damage

interface IDamager {

    fun canDamage(damageable: IDamageable): Boolean = true

    fun onDamageInflictedTo(damageable: IDamageable) {}
}
