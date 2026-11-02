package com.megaman.maverick.game.players

import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.players.isPlayer
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

val IGameEntity?.asPlayer: MegaGameEntity?
    get() = (this as? MegaGameEntity)?.takeIf { it.isPlayer }

// TODO: the owner chain goes 2 levels deep, as Megaman.canBeDamagedBy did; a longer chain does not reach the player, and
//  a shot from it counts as an enemy's (item 030)
val IGameEntity.rootPlayer: MegaGameEntity?
    get() {
        asPlayer?.let { return it }
        val owner = (this as? IOwnable<*>)?.owner
        return owner.asPlayer ?: (owner as? IOwnable<*>)?.owner.asPlayer
    }
