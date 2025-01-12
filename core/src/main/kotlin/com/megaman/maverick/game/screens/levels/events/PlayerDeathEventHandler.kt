package com.megaman.maverick.game.screens.levels.events

import com.badlogic.gdx.graphics.g2d.Batch
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.utils.Fade
import com.megaman.maverick.game.screens.utils.Fade.FadeType
import com.megaman.maverick.game.utils.extensions.toGameRectangle

class PlayerDeathEventHandler(private val game: MegamanMaverickGame) : Initializable, Updatable, Resettable,
    IDrawable<Batch> {

    companion object {
        const val TAG = "PlayerDeathEventHandler"
        private const val ON_DEATH_DELAY = 4f
        private const val FADE_OUT_DUR = 0.25f
    }

    val finished: Boolean
        get() = deathTimer.isFinished() && fadeout.isFinished()

    private val fadeout = Fade(FadeType.FADE_OUT, FADE_OUT_DUR)
    private val deathTimer = Timer(ON_DEATH_DELAY).setToEnd()

    override fun init() {
        GameLogger.debug(TAG, "init()")

        deathTimer.reset()

        val black = game.assMan.getTextureRegion(TextureAsset.UI_1.source, ConstKeys.BLACK)
        fadeout.setRegion(black)
        fadeout.reset()

        game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))

        game.megaman.body.physics.gravityOn = false
        game.megaman.ready = false

        game.audioMan.playSound(SoundAsset.DEFEAT_SOUND)
    }

    override fun update(delta: Float) {
        deathTimer.update(delta)

        if (deathTimer.isJustFinished()) {
            GameLogger.debug(TAG, "update(): death timer just finished, init fade out")
            fadeout.init()
        }

        if (deathTimer.isFinished()) {
            val bounds = game.getUiCamera().toGameRectangle()
            fadeout.setPosition(bounds.getX(), bounds.getY())
            fadeout.setSize(bounds.getWidth(), bounds.getHeight())
            fadeout.update(delta)

            if (fadeout.isJustFinished()) {
                GameLogger.debug(TAG, "update(): fade out just finished, submit PLAYER_DONE_DYIN event")
                game.eventsMan.submitEvent(Event(EventType.PLAYER_DONE_DYIN))
            }
        }
    }

    override fun draw(drawer: Batch) {
        fadeout.draw(drawer)
    }

    override fun reset() {
        GameLogger.debug(TAG, "reset()")
        deathTimer.reset()
        fadeout.reset()
    }

    fun setToEnd() {
        GameLogger.debug(TAG, "setToEnd()")
        deathTimer.setToEnd()
        fadeout.setToEnd()
    }

    fun setInactive() {
        deathTimer.setToEnd()
        fadeout.reset()
    }
}
