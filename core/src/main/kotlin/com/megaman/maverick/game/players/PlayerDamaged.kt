package com.megaman.maverick.game.players

import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class PlayerDamaged {

    lateinit var player: MegaGameEntity
        internal set

    lateinit var damager: IDamagerEntity
        internal set
}
