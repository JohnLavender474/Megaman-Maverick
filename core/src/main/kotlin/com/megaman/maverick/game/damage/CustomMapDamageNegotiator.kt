package com.megaman.maverick.game.damage

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.damage.IDamager
import kotlin.reflect.KClass

class CustomMapDamageNegotiator(val map: ObjectMap<KClass<out IDamager>, DamageNegotiation?>): IDamageNegotiator {

    override fun get(damager: IDamager) = map.get(damager::class)?.get(damager) ?: 0
}
