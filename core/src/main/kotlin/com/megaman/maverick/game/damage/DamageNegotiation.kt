package com.megaman.maverick.game.damage

import com.maverick.game.engine.damage.IDamagerEntity

class DamageNegotiation(var negotation: (IDamagerEntity) -> Int) {

    constructor(damage: Int) : this({ damage })

    fun get(damager: IDamagerEntity) = negotation(damager)
}

fun dmgNeg(negotation: (IDamagerEntity) -> Int) = DamageNegotiation(negotation)

fun dmgNeg(damage: Int) = DamageNegotiation(damage)
