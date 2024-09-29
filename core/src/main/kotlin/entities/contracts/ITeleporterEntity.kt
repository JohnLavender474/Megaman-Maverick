package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.mega.game.engine.entities.contracts.IBodyEntity

interface ITeleporterEntity {

    fun teleportEntity(entity: IBodyEntity)
}