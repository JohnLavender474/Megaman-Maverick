package com.megaman.maverick.game.entities.items

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.events.EventType

class HealthBulb(game: MegamanMaverickGame) : AbstractEnergyItem(game) {

    companion object {
        const val TAG = "HealthBulb"
    }

    override fun contactWithPlayer(megaman: Megaman) {
        GameLogger.debug(TAG, "contactWithPlayer(): megaman=$megaman")

        destroy()

        game.eventsMan.submitEvent(
            Event(
                EventType.ADD_PLAYER_HEALTH, props(ConstKeys.VALUE pairTo if (large) LARGE_AMOUNT else SMALL_AMOUNT)
            )
        )
    }

    override fun getTag() = TAG
}
