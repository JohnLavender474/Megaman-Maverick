package com.megaman.maverick.game.players

import com.maverick.game.engine.damage.IDamager

interface IPlayerDamageListener {

    fun onPlayerDamaged(damager: IDamager, player: MegaPlayer)
}
