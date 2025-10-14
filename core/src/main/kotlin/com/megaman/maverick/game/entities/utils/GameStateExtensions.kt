package com.megaman.maverick.game.entities.utils

import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.state.GameState

val GameState.hardMode: Boolean
    get() = getDifficultyMode() == DifficultyMode.HARD
