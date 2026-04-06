package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.Fade
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

    private val fadeIn = Fade(Fade.FadeType.FADE_IN, FADE_IN_DUR)
    private val fadeOut = Fade(Fade.FadeType.FADE_OUT, FADE_OUT_DUR)

    private val showTimer = Timer(SHOW_DUR)
    private val doneTimer = Timer(DONE_DUR)

    private val slideQueue = Queue<Array<MegaFontHandle>>()
    private var currentLines = Array<MegaFontHandle>()
    private var slideState = SlideState.FADE_IN

    private var done = false

    override fun show() {
        GameLogger.debug(TAG, "show()")

        game.audioMan.playMusic(MusicAsset.MMX3_ZERO_THEME_MUSIC)
        game.getUiCamera().setToDefaultPosition()

        val black = game.assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
        val w = ConstVals.VIEW_WIDTH * ConstVals.PPM
        val h = ConstVals.VIEW_HEIGHT * ConstVals.PPM

        fadeIn.setRegion(black)
        fadeIn.setX(0f)
        fadeIn.setY(0f)
        fadeIn.setWidth(w)
        fadeIn.setHeight(h)

        fadeOut.setRegion(black)
        fadeOut.setX(0f)
        fadeOut.setY(0f)
        fadeOut.setWidth(w)
        fadeOut.setHeight(h)

        slideQueue.clear()

        done = false
        doneTimer.reset()

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
            return
        }

        currentLines = slideQueue.removeFirst()
        slideState = SlideState.FADE_IN

        fadeIn.init()
        fadeOut.init()

        showTimer.reset()
    }

    override fun render(delta: Float) {
        if (done) {
            doneTimer.update(delta)
            if (doneTimer.isJustFinished()) onCompletion()
            return
        }

        when (slideState) {
            SlideState.FADE_IN -> {
                fadeIn.update(delta)
                if (fadeIn.isFinished()) slideState = SlideState.SHOW
            }

            SlideState.SHOW -> {
                showTimer.update(delta)
                if (showTimer.isFinished()) slideState = SlideState.FADE_OUT
            }

            SlideState.FADE_OUT -> {
                fadeOut.update(delta)
                if (fadeOut.isFinished()) advanceSlide()
            }
        }
    }

    override fun draw(drawer: Batch) {
        drawer.projectionMatrix = game.getUiCamera().combined
        drawer.begin()
        if (!done) currentLines.forEach { it.draw(drawer) }
        when (slideState) {
            SlideState.FADE_IN -> fadeIn.draw(drawer)
            SlideState.FADE_OUT -> fadeOut.draw(drawer)
            else -> {}
        }
        drawer.end()
    }
}
