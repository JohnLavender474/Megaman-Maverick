package com.megaman.maverick.game.damage

import com.mega.game.engine.damage.IDamager

class DamageNegotiation(var negotation: (IDamager) -> Int) {

    constructor(damage: Int) : this({ damage })

    fun get(damager: IDamager) = negotation(damager)
}

fun dmgNeg(negotation: (IDamager) -> Int) = DamageNegotiation(negotation)

fun dmgNeg(damage: Int) = DamageNegotiation(damage)
