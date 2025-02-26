package com.megaman.maverick.game.levels

import com.megaman.maverick.game.state.GameState

object LevelUtils {

    fun isInfernoManLevelFrozen(state: GameState) = state.isLevelDefeated(LevelDefinition.GLACIER_MAN) &&
        !state.isLevelDefeated(LevelDefinition.INFERNO_MAN)
}
