package com.maverick.game.engine.players

import com.maverick.game.engine.entities.IGameEntity

class PlayerNotRegisteredException(val entity: IGameEntity) : IllegalStateException(
    "$entity is a player class but is not in a slot. Read the slot only from a registered player: get the player " +
        "from the players service or through asPlayer, or read the nullable playerSlot."
)
