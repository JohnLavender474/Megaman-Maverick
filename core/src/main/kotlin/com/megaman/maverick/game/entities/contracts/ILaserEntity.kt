package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.entities.IGameEntity
import com.megaman.maverick.game.entities.blocks.Block

interface ILaserEntity : IGameEntity {

    fun isIgnoringBlock(block: Block): Boolean
}
