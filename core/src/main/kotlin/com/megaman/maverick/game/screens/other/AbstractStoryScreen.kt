package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.utils.Fade
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

abstract class AbstractStoryScreen(protected val game: MegamanMaverickGame) : BaseScreen() {

    protected open val fadeInDur = 1f
    protected open val showDur= 3.5f
    protected open val fadeOutDur = 1.5f
    protected open val doneDur = 1f
    protected open val lineSpacing = 1.5f

    protected abstract val music: MusicAsset?
    protected abstract val slides: Array<Array<String>>

    protected abstract fun onCompletion()

    private enum class SlideState { FADE_IN, SHOW, FADE_OUT }

    private lateinit var fadeIn: Fade
    private lateinit var fadeOut: Fade
    private lateinit var showTimer: Timer
    private lateinit var doneTimer: Timer

    private val slideQueue = Queue<Array<MegaFontHandle>>()
    private var currentLines = Array<MegaFontHandle>()
    private var slideState = SlideState.FADE_IN
    private var done = false

    override fun show() {
        GameLogger.debug(javaClass.simpleName, "show()")

        music?.let { game.audioMan.playMusic(it) }
        game.getUiCamera().setToDefaultPosition()

        val black = game.assMan.getTextureRegion(TextureAsset.COLORS.source, ConstKeys.BLACK)
        val w = ConstVals.VIEW_WIDTH * ConstVals.PPM
        val h = ConstVals.VIEW_HEIGHT * ConstVals.PPM

        fadeIn = Fade(Fade.FadeType.FADE_IN, fadeInDur).apply {
            setRegion(black); setX(0f); setY(0f); setWidth(w); setHeight(h)
        }
        fadeOut = Fade(Fade.FadeType.FADE_OUT, fadeOutDur).apply {
            setRegion(black); setX(0f); setY(0f); setWidth(w); setHeight(h)
        }
        showTimer = Timer(showDur)
        doneTimer = Timer(doneDur)

        slideQueue.clear()

        done = false
        doneTimer.reset()

        slides.forEach { lines ->
            val n = lines.size
            val spacing = lineSpacing * ConstVals.PPM
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

    private fun advanceSlide() {
        if (slideQueue.isEmpty) {
            done = true
            game.audioMan.fadeOutMusic(doneDur)
            return
        }

        currentLines = slideQueue.removeFirst()
        slideState = SlideState.FADE_IN
        fadeIn.init()
        fadeOut.init()
        showTimer.reset()
    }

    override fun render(delta: Float) {
        if (!done && game.controllerPoller.isJustReleased(MegaControllerButton.START)) {
            done = true
            game.audioMan.playMusic(SoundAsset.SELECT_PING_SOUND, false)
        }

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
