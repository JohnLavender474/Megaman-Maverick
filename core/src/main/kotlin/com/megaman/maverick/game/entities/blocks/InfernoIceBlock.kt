package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.levels.LevelUtils

class InfernoIceBlock(game: MegaServices) : IceBlock(game) {

    override fun canSpawn(spawnProps: Properties) = super.canSpawn(spawnProps) &&
        LevelUtils.isInfernoManLevelFrozen(game.state)
}
