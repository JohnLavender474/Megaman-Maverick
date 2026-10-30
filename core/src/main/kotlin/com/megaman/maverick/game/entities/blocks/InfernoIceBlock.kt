package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.levels.LevelUtils

class InfernoIceBlock(services: MegaServices) : IceBlock(services) {

    override fun canSpawn(spawnProps: Properties) = super.canSpawn(spawnProps) &&
        LevelUtils.isInfernoManLevelFrozen(services.gameState)
}
