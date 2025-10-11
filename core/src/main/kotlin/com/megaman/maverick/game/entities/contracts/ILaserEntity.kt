package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.entities.IGameEntity

interface ILaserEntity : IGameEntity {

    fun isLaserIgnoring(entity: IGameEntity): Boolean
}
