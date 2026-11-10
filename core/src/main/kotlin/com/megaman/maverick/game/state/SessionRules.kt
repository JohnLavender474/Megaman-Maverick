package com.megaman.maverick.game.state

import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.players.rootPlayer

data class SessionRules(
    val friendlyFire: Boolean,
    val viewEdges: ViewEdgeRules = ViewEdgeRules(),
    val slotRespawn: Boolean = false
) {

    fun isFriendlyFire(source: IDamagerEntity, target: MegaGameEntity): Boolean {
        if (friendlyFire) return false
        val sourcePlayer = source.rootPlayer ?: return false
        val targetPlayer = target.rootPlayer ?: return false
        return sourcePlayer != targetPlayer
    }
}

enum class GameMode(val rules: SessionRules) {
    SINGLE(SessionRules(friendlyFire = false)),
    COOP(
        SessionRules(
            friendlyFire = false,
            viewEdges = ViewEdgeRules(bottom = ViewEdgeRule.WARP, delay = 1f),
            slotRespawn = true
        )
    ),
    PVP(SessionRules(friendlyFire = true))
}
