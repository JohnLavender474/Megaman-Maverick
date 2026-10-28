package com.megaman.maverick.game.damage

import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.damage.IDamagerEntity
import kotlin.reflect.KClass

class CustomMapDamageNegotiator(val map: ObjectMap<KClass<out IDamagerEntity>, DamageNegotiation?>): IDamageNegotiator {

    override fun get(damager: IDamagerEntity) = map.get(damager::class)?.get(damager) ?: 0
}
