package com.megaman.maverick.game.damage

import com.mega.game.engine.common.interfaces.ISizable
import com.mega.game.engine.damage.IDamager

class SelfSizeDamageNegotiator(var sizable: ISizable): IDamageNegotiator {

    override fun get(damager: IDamager) = StandardDamageNegotiator.get(sizable.size, damager)
}
