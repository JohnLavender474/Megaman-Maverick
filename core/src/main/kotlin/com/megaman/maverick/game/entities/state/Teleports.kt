package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.IGameEntity
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity

val IGameEntity.teleporter: ITeleporterEntity?
    get() = getComponent(TeleportableComponent::class)?.teleporter

val IGameEntity.teleporting: Boolean
    get() = teleporter != null

fun IGameEntity.beginTeleport(teleporter: ITeleporterEntity) {
    val teleportable = require(TeleportableComponent::class)
    teleportable.onStart?.invoke(teleporter)
    teleportable.teleporter = teleporter
}

fun IGameEntity.endTeleport() {
    val teleportable = require(TeleportableComponent::class)
    teleportable.onEnd?.invoke()
    teleportable.teleporter = null
}
