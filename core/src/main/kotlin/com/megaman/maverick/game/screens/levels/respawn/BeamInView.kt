package com.megaman.maverick.game.screens.levels.respawn

import com.badlogic.gdx.graphics.g2d.Batch
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.IDrawable
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.utils.Fade
import com.megaman.maverick.game.screens.utils.Fade.FadeType
import com.megaman.maverick.game.utils.extensions.toGameRectangle

class BeamInView(private val game: MegamanMaverickGame) : Initializable, Updatable, Resettable, IDrawable<Batch> {

    companion object {
        const val TAG = "BeamInView"
        private const val PRE_BEAM_DUR = 1f
        private const val BLINK_READY_DUR = 0.125f
        private const val FADE_IN_DUR = 0.25f
    }

    // TODO: the beam-in acts on the lead player in slot 0; with more players, each player needs its own (item 047)
    private val player: IPlayer
        get() = game.players.get(PlayerSlot(0))!!

    private val blinkTimer = Timer(BLINK_READY_DUR).setToEnd()
    private val preBeamTimer = Timer(PRE_BEAM_DUR).setToEnd()

    private val beam = BeamDown(game.assets) { game.addDrawable(it) }

    private lateinit var ready: MegaFontHandle

    private val fadein = Fade(FadeType.FADE_IN, FADE_IN_DUR)

    private var initialized = false
    private var showReadyText = false

    var finished = true
        private set

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")

        if (!initialized) {
            ready = MegaFontHandle(
                { ConstKeys.READY.uppercase() },
                positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
                positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f,
                priority = DrawingPriority(DrawingSection.FOREGROUND, 15)
            )

            val black = game.assets.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
            fadein.setRegion(black)

            initialized = true
        }

        showReadyText = false

        blinkTimer.reset()
        preBeamTimer.reset()
        beam.stop()

        fadein.init()

        finished = false
    }

    override fun update(delta: Float) {
        game.levelState.megamanBeamCenter = beam.center

        when {
            !fadein.isFinished() -> {
                val bounds = game.getUiCamera().toGameRectangle()
                fadein.setPosition(bounds.getX(), bounds.getY())
                fadein.setSize(bounds.getWidth(), bounds.getHeight())
                fadein.update(delta)
            }
            !preBeamTimer.isFinished() -> preBeam(delta)
            !beam.finished -> beamDown(delta)
        }

        blinkTimer.update(delta)
        if (blinkTimer.isFinished()) {
            showReadyText = !showReadyText
            blinkTimer.reset()
        }
    }

    override fun reset() {
        GameLogger.debug(TAG, "reset()")

        preBeamTimer.setToEnd()
        beam.stop()
        fadein.setToEnd()

        finished = true
    }

    private fun preBeam(delta: Float) {
        preBeamTimer.update(delta)
        if (preBeamTimer.isJustFinished()) {
            GameLogger.debug(TAG, "preBeam(): just finished")
            beam.start(player)
        }
    }

    private fun beamDown(delta: Float) {
        beam.update(delta)
        if (beam.finished) {
            GameLogger.debug(TAG, "beamDown(): just finished")

            showReadyText = false

            finished = true
        }
    }

    override fun draw(drawer: Batch) = when {
        fadein.isFinished() && showReadyText -> ready.draw(drawer)
        else -> fadein.draw(drawer)
    }
}
