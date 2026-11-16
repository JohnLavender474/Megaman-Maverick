package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.IGameComponent
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.state.IGameStateListener

class PlayerProgressComponent(
    private val onHeartTanksChanged: () -> Unit,
    private val onCurrencyAdded: () -> Unit
) : IGameComponent, IGameStateListener {

    override fun onAddHeartTank(heartTank: MegaHeartTank) = onHeartTanksChanged()

    override fun onRemoveHeartTank(heartTank: MegaHeartTank) = onHeartTanksChanged()

    override fun onAddCurrency(value: Int) = onCurrencyAdded()
}
