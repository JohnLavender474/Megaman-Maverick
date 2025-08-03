package com.megaman.maverick.game.entities.blocks

import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.levels.LevelUtils

class InfernoIceBlock(game: MegamanMaverickGame) : IceBlock(game) {

    override fun canSpawn(spawnProps: Properties) = super.canSpawn(spawnProps) &&
        LevelUtils.isInfernoManLevelFrozen(game.state)
}
