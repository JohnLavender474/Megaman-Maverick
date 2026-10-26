package com.megaman.maverick.game.players

import com.maverick.game.engine.damage.IDamager

class PlayerDamaged {

    lateinit var player: MegaPlayer
        internal set

    lateinit var damager: IDamager
        internal set
}
