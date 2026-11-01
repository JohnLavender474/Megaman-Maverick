package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.IGameComponent
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.IGameStateListener

class PlayerProgressComponent(
    private val onLevelDefeated: (LevelDefinition) -> Unit,
    private val onHeartTanksChanged: () -> Unit,
    private val onCurrencyAdded: () -> Unit
) : IGameComponent, IGameStateListener {

    override fun onAddLevelDefeated(level: LevelDefinition) = onLevelDefeated(level)

    override fun onAddHeartTank(heartTank: MegaHeartTank) = onHeartTanksChanged()

    override fun onRemoveHeartTank(heartTank: MegaHeartTank) = onHeartTanksChanged()

    override fun onAddCurrency(value: Int) = onCurrencyAdded()
}
