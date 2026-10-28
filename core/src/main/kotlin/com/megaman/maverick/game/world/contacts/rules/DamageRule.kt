package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.world.body.*

class DamageRule(private val debugFilter: (IFixture, IFixture) -> Boolean) : IContactRule {

    companion object {
        const val TAG = "DamageRule"
    }

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a, b)

    private fun begin(damagerFixture: IFixture, damageableFixture: IFixture) {
        val damager = damagerFixture.getEntity() as IDamagerEntity
        val damageable = damageableFixture.getEntity() as IDamageableEntity

        damagerFixture.receiver(Receivers.HIT_BY_DAMAGEABLE)?.invoke(damageable, ProcessState.BEGIN)

        val canBeDamaged = damageable.canBeDamagedBy(damager)
        printDebugLog(damagerFixture, damageableFixture, "canBeDamaged=$canBeDamaged")

        val canDamage = damager.canDamage(damageable)
        printDebugLog(damagerFixture, damageableFixture, "canDamage=$canDamage")
        if (canBeDamaged && canDamage) {
            val takeDamageFrom = damageable.takeDamageFrom(damager)
            printDebugLog(damagerFixture, damageableFixture, "takeDamageFrom=$takeDamageFrom")
            damager.onDamageInflictedTo(damageable)
        }
    }

    private fun continueContact(damagerFixture: IFixture, damageableFixture: IFixture) {
        val damager = damagerFixture.getEntity() as IDamagerEntity
        val damageable = damageableFixture.getEntity() as IDamageableEntity

        damagerFixture.receiver(Receivers.HIT_BY_DAMAGEABLE)?.invoke(damageable, ProcessState.CONTINUE)

        if (damageable.canBeDamagedBy(damager) && damager.canDamage(damageable)) {
            damageable.takeDamageFrom(damager)
            damager.onDamageInflictedTo(damageable)
        }
    }

    private fun printDebugLog(damagerFixture: IFixture, damageableFixture: IFixture, log: String) {
        if (debugFilter.invoke(damagerFixture, damageableFixture)) GameLogger.debug(TAG, log)
    }
}
