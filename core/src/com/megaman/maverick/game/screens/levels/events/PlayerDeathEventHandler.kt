package com.megaman.maverick.game.screens.levels.events

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.events.EventType

class PlayerDeathEventHandler(private val game: MegamanMaverickGame) : Initializable, Updatable, Resettable {

    companion object {
        const val TAG = "PlayerDeathEventHandler"
        private const val ON_DEATH_DELAY = 4f
    }

    val finished: Boolean
        get() = deathTimer.isFinished()

    private val deathTimer = Timer(ON_DEATH_DELAY).setToEnd()

    override fun init() {
        GameLogger.debug(TAG, "Initializing...")
        deathTimer.reset()
        game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))
        game.megaman.body.physics.gravityOn = false
        game.megaman.ready = false
        game.audioMan.playSound(SoundAsset.DEFEAT_SOUND)
        GameLogger.debug(TAG, "Initialized")
    }

    override fun update(delta: Float) {
        deathTimer.update(delta)
        if (deathTimer.isJustFinished()) {
            GameLogger.debug(TAG, "Player death timer just finished")
            game.eventsMan.submitEvent(Event(EventType.PLAYER_DONE_DYIN))
        }
    }

    override fun reset() {
        deathTimer.setToEnd(allowJustFinished = false)
    }
}
