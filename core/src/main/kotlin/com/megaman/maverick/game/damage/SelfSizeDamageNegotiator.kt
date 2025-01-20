package com.megaman.maverick.game.damage

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.interfaces.ISizable
import com.mega.game.engine.damage.IDamager
import kotlin.reflect.KClass

class SelfSizeDamageNegotiator(
    var sizable: ISizable,
    var overrides: ObjectMap<KClass<out IDamager>, DamageNegotiation?> = ObjectMap()
) : IDamageNegotiator {

    override fun get(damager: IDamager): Int {
        val key = damager::class
        return when {
            overrides.containsKey(key) -> overrides[key]?.get(damager) ?: 0
            else -> StandardDamageNegotiator.get(sizable.size, damager)
        }
    }
}
