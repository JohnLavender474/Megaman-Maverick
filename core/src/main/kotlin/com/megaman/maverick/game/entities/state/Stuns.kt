package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.players.playerSlot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.utils.misc.StunType

private const val TAG = "Stuns"

val IGameEntity.stunned: Boolean
    get() = getComponent(StunComponent::class)?.stunned == true

fun IGameEntity.onStunPlayerEvent(event: Event) {
    GameLogger.debug(TAG, "onStunPlayerEvent(): called")

    val targets = event.getProperty(PlayerEventKeys.SLOTS)
    if (targets == null || !targets.contains(playerSlot, false)) {
        GameLogger.debug(TAG, "onStunPlayerEvent(): not a target: $targets")
        return
    }

    val stun = require(StunComponent::class)

    if (stun.stunned) {
        GameLogger.debug(TAG, "onStunPlayerEvent(): do not stun because the entity is already stunned")
        return
    }

    val stunType = event.getProperty(ConstKeys.TYPE, StunType::class)!!
    if (stunType == StunType.STUN_BOUNCE_IF_ON_SURFACE && !stun.onSurface()) {
        GameLogger.debug(TAG, "onStunPlayerEvent(): do not stun: type=$stunType and the entity is not on a surface")
        return
    }

    stun.onStun()
    stun.start()
}
