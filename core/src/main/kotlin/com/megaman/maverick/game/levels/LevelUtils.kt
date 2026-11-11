package com.megaman.maverick.game.levels

import com.megaman.maverick.game.state.GameState

object LevelUtils {

    fun isInfernoManLevelFrozen(state: GameState) = state.isLevelCompleted(LevelDefinition.GLACIER_MAN) &&
        !state.isLevelCompleted(LevelDefinition.INFERNO_MAN)
}
