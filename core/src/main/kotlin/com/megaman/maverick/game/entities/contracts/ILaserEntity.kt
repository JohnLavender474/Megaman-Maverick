package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.world.body.IFixture

interface ILaserEntity : IGameEntity {

    fun isLaserIgnoring(entity: IGameEntity): Boolean

    fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}
}
