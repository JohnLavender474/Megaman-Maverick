package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.entities.contracts.IBodyEntity

interface ITeleporterEntity {

    fun shouldTeleport(entity: IBodyEntity) = !isTeleporting(entity)

    fun isTeleporting(entity: IBodyEntity): Boolean

    fun teleport(entity: IBodyEntity)
}
