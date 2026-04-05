package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

class EndingScreen(private val game: MegamanMaverickGame) : BaseScreen() {

    companion object {
        const val TAG = "EndingScreen"

        private const val FADE_IN_DUR = 1f
        private const val SHOW_DUR = 5f
        private const val FADE_OUT_DUR = 1.5f
        private const val DONE_DUR = 1f

        private const val LINE_SPACING = 1.5f

        private val STORY_SLIDES = gdxArrayOf(
            gdxArrayOf("DR. WILY HAS BEEN DEFEATED!"),
            gdxArrayOf(
                "THANKS TO MEGA MAN'S BRAVERY,",
                "THE WORLD IS SAFE ONCE MORE."
            ),
            gdxArrayOf(
                "DR. WILY KNELT BEFORE MEGA MAN,",
                "BEGGING FOR MERCY AS ALWAYS."
            ),
            gdxArrayOf(
                "MEGA MAN COULD ONLY SIGH.",
                "WHEN WILL DR. WILY EVER",
                "CHANGE HIS WAYS?"
            ),
            gdxArrayOf(
                "AS LONG AS THERE IS EVIL,",
                "MEGA MAN STANDS EVER READY"
            ),
            gdxArrayOf(
                "EVEN AS A MAVERICK",
                "IN THE EYES OF THE WORLD..."
            )
        )
    }

    private enum class SlideState { FADE_IN, SHOW, FADE_OUT }

    private val fadeInTimer = Timer(FADE_IN_DUR)
    private val showTimer = Timer(SHOW_DUR)
    private val fadeOutTimer = Timer(FADE_OUT_DUR)

    private val slideQueue = Queue<Array<MegaFontHandle>>()
    private var currentLines = Array<MegaFontHandle>()
    private var slideState = SlideState.FADE_IN

    private var done = false
    private val doneTimer = Timer(DONE_DUR)

    // Alpha of the black overlay: 1 = fully black (text hidden), 0 = transparent (text visible).
    // Using an overlay instead of font alpha sidesteps unreliable BitmapFont alpha behavior.
    private var overlayAlpha = 1f
    private val overlay = GameSprite()

    override fun show() {
        GameLogger.debug(TAG, "show()")

        game.audioMan.playMusic(MusicAsset.MMX3_ZERO_THEME_MUSIC)
        game.getUiCamera().setToDefaultPosition()

        val black = game.assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
        overlay.setRegion(black)
        overlay.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)

        slideQueue.clear()

        done = false
        doneTimer.reset()

        overlayAlpha = 1f

        STORY_SLIDES.forEach { lines ->
            val n = lines.size
            val spacing = LINE_SPACING * ConstVals.PPM
            val centerY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
            val startY = centerY + ((n - 1) / 2f) * spacing
            val handles = lines.mapIndexed { i, text ->
                MegaFontHandle(
                    text = text,
                    positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
                    positionY = startY - i * spacing
                )
            }.toGdxArray()
            slideQueue.addLast(handles)
        }

        advanceSlide()
    }

    private fun onCompletion() = game.setCurrentScreen(ScreenEnum.CREDITS_SCREEN.name)

    private fun advanceSlide() {
        if (slideQueue.isEmpty) {
            done = true
            game.audioMan.fadeOutMusic(DONE_DUR)
            overlayAlpha = 1f
            return
        }

        currentLines = slideQueue.removeFirst()
        slideState = SlideState.FADE_IN

        overlayAlpha = 1f

        fadeInTimer.reset()
        showTimer.reset()
        fadeOutTimer.reset()
    }

    override fun render(delta: Float) {
        if (done) {
            doneTimer.update(delta)
            if (doneTimer.isJustFinished()) onCompletion()
            return
        }

        when (slideState) {
            SlideState.FADE_IN -> {
                fadeInTimer.update(delta)
                overlayAlpha = 1f - fadeInTimer.getRatio()
                if (fadeInTimer.isFinished()) slideState = SlideState.SHOW
            }

            SlideState.SHOW -> {
                showTimer.update(delta)
                overlayAlpha = 0f
                if (showTimer.isFinished()) slideState = SlideState.FADE_OUT
            }

            SlideState.FADE_OUT -> {
                fadeOutTimer.update(delta)
                overlayAlpha = fadeOutTimer.getRatio()
                if (fadeOutTimer.isFinished()) advanceSlide()
            }
        }
    }

    override fun draw(drawer: Batch) {
        drawer.projectionMatrix = game.getUiCamera().combined
        drawer.begin()
        if (!done) currentLines.forEach { it.draw(drawer) }
        overlay.setAlpha(overlayAlpha)
        overlay.draw(drawer)
        drawer.end()
    }
}
