package com.megaman.maverick.game.damage

import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.interfaces.ISizable
import com.maverick.game.engine.damage.IDamagerEntity
import kotlin.reflect.KClass

class SelfSizeDamageNegotiator(
    var sizable: ISizable,
    var overrides: ObjectMap<KClass<out IDamagerEntity>, DamageNegotiation?> = ObjectMap()
) : IDamageNegotiator {

    override fun get(damager: IDamagerEntity): Int {
        val key = damager::class
        return when {
            overrides.containsKey(key) -> overrides[key]?.get(damager) ?: 0
            else -> StandardDamageNegotiator.get(sizable.size, damager)
        }
    }
}
