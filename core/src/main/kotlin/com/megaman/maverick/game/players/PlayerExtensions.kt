package com.megaman.maverick.game.players

import com.maverick.game.engine.players.isPlayer
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

val Any?.asPlayer: MegaGameEntity?
    get() = (this as? MegaGameEntity)?.takeIf { it.isPlayer }
