package com.megaman.maverick.game.entities.sensors

import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.levels.LevelDefinition

class InfernoDeath(game: MegamanMaverickGame): Death(game) {

    companion object {
        const val TAG = "InfernoDeath"
    }

    override fun canSpawn(spawnProps: Properties) = super.canSpawn(spawnProps) &&
        !game.state.isLevelDefeated(LevelDefinition.GLACIER_MAN)

    override fun getTag() = TAG
}
