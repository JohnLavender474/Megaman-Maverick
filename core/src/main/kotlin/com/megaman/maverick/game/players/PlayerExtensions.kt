package com.megaman.maverick.game.players

import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.isPlayer
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.events.PlayerEventKeys

val IGameEntity?.asPlayer: IPlayer?
    get() = (this as? IPlayer)?.takeIf { it.isPlayer }

// TODO: the owner chain goes 2 levels deep, as Megaman.canBeDamagedBy did; a longer chain does not reach the player, and
//  a shot from it counts as an enemy's (item 030)
val IGameEntity.rootPlayer: IPlayer?
    get() {
        asPlayer?.let { return it }
        val owner = (this as? IOwnable<*>)?.owner
        return owner.asPlayer ?: (owner as? IOwnable<*>)?.owner.asPlayer
    }

fun Event.isDeathOf(player: IPlayer): Boolean =
    getProperty(PlayerEventKeys.SLOT, PlayerSlot::class) == player.slot
