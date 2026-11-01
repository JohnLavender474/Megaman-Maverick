package com.megaman.maverick.game.entities.megaman.components

import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.state.PlayerProgressComponent
import com.megaman.maverick.game.levels.LevelRewards

internal fun Megaman.definePlayerProgressComponent() = PlayerProgressComponent(
    onLevelDefeated = { level -> services.levelState.weaponsAttained = LevelRewards.weaponsFor(level) },
    onHeartTanksChanged = { setMaxHealthFromHeartTanks() },
    onCurrencyAdded = {
        if (services.gameState.getCurrency() < services.gameState.getMaxCurrency())
            requestToPlaySound(SoundAsset.CURRENCY_PICKUP_SOUND, false)
    }
)

// TODO: the max health is a stored copy that is set when a heart tank changes; a max that is read through from
//  the game state on each access needs a change to the engine Points (backlog candidate "max health read through
//  from the profile", item 029)
private fun Megaman.setMaxHealthFromHeartTanks() {
    val heartTanks = MegaHeartTank.entries.count { services.gameState.containsHeartTank(it) }
    setMaxHealth(MegamanValues.START_HEALTH + heartTanks * MegaHeartTank.HEALTH_BUMP)
}
