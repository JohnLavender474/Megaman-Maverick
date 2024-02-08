package com.megaman.maverick.game.entities.contracts

import com.engine.entities.contracts.IBodyEntity

interface ITeleporterEntity {

    fun teleportEntity(entity: IBodyEntity)
}