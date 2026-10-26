package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.IGameEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity

val IGameEntity.teleporter: ITeleporterEntity?
    get() = getComponent(TeleportableComponent::class)?.teleporter

val IGameEntity.teleporting: Boolean
    get() = teleporter != null

fun IGameEntity.beginTeleport(teleporter: ITeleporterEntity) {
    val teleportable = require(TeleportableComponent::class)
    (getProperty(ConstKeys.ON_TELEPORT_START) as? (ITeleporterEntity) -> Unit)?.invoke(teleporter)
    teleportable.teleporter = teleporter
}

fun IGameEntity.endTeleport() {
    val teleportable = require(TeleportableComponent::class)
    (getProperty(ConstKeys.ON_TELEPORT_END) as? () -> Unit)?.invoke()
    teleportable.teleporter = null
}
