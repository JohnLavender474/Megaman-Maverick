package com.megaman.maverick.game.damage

import com.maverick.game.engine.damage.IDamager

interface IDamageNegotiator {

    fun get(damager: IDamager): Int
}
