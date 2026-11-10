package com.megaman.maverick.game.players

import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.players.IPlayer

class PlayerDamaged {

    lateinit var player: IPlayer
        internal set

    lateinit var damager: IDamagerEntity
        internal set
}
