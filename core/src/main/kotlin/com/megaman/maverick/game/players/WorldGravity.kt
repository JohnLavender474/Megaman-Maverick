package com.megaman.maverick.game.players

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.players.forEach

class WorldGravity(private val players: MegaPlayers) {

    var direction = Direction.UP
        private set

    fun set(direction: Direction) {
        this.direction = direction
        players.forEach { it.direction = direction }
    }
}
