package com.megaman.maverick.game.state

import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition

interface IGameStateListener {

    fun onAddLevelDefeated(level: LevelDefinition)

    fun onRemoveLevelDefeated(level: LevelDefinition)

    fun onAddHeartTank(heartTank: MegaHeartTank)

    fun onRemoveHeartTank(heartTank: MegaHeartTank)

    fun onPutHealthTank(healthTank: MegaHealthTank)

    fun onRemoveHealthTank(healthTank: MegaHealthTank)

    fun onAddEnhancement(enhancement: MegaEnhancement)

    fun onRemoveEnhancement(enhancement: MegaEnhancement)
}
