package com.megaman.maverick.game.damage

import com.maverick.game.engine.damage.IDamagerEntity

interface IDamageNegotiator {

    fun get(damager: IDamagerEntity): Int
}
