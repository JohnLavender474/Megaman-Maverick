package com.megaman.maverick.game.state.saves

import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameState

class SaveFile(
    val savedAt: String,
    val levelsCompleted: Array<LevelDefinition>,
    val heartTanks: Array<MegaHeartTank>,
    val healthTanks: Array<MegaHealthTank>,
    val enhancements: Array<MegaEnhancement>,
    val currency: Int,
    val difficulty: DifficultyMode
) {

    companion object {

        fun of(state: GameState, savedAt: String): SaveFile {
            val levels = Array<LevelDefinition>()
            LevelDefinition.entries.forEach { if (state.isLevelCompleted(it)) levels.add(it) }
            val heartTanks = Array<MegaHeartTank>()
            MegaHeartTank.entries.forEach { if (state.containsHeartTank(it)) heartTanks.add(it) }
            val healthTanks = Array<MegaHealthTank>()
            MegaHealthTank.entries.forEach { if (state.containsHealthTank(it)) healthTanks.add(it) }
            val enhancements = Array<MegaEnhancement>()
            MegaEnhancement.entries.forEach { if (state.containsEnhancement(it)) enhancements.add(it) }
            return SaveFile(
                savedAt = savedAt,
                levelsCompleted = levels,
                heartTanks = heartTanks,
                healthTanks = healthTanks,
                enhancements = enhancements,
                currency = state.getCurrency(),
                difficulty = state.getDifficultyMode()
            )
        }
    }

    fun applyTo(state: GameState) {
        state.reset()
        levelsCompleted.forEach { state.addLevelCompleted(it) }
        heartTanks.forEach { state.addHeartTank(it) }
        healthTanks.forEach { state.putHealthTank(it) }
        enhancements.forEach { state.addEnhancement(it) }
        if (currency > 0) state.addCurrency(currency)
        state.setDifficultyMode(difficulty)
    }
}
