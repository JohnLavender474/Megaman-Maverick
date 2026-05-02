package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
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
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.Fade
import com.megaman.maverick.game.screens.utils.Fade.FadeType
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

object CreditsLoader {

    const val CREDITS_SOURCE = "credits.txt"

    fun load(): Queue<MegaFontHandle> {
        val credits = Queue<MegaFontHandle>()
        val inputStream = InternalFileHandleResolver().resolve(CREDITS_SOURCE).read()
        val reader = inputStream.reader()
        reader.forEachLine { line -> credits.addLast(MegaFontHandle(text = line.replace("#", "").uppercase())) }
        return credits
    }
}

class CreditsScreen(
    private val game: MegamanMaverickGame,
    var onCompletion: () -> Unit = { game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name) }
) : BaseScreen() {

    companion object {
        const val TAG = "CreditsScreen"
        private const val DELAY_TIME = 1f
        private const val SPEED = 2.5f
        private const val LERP_ALPHA = 10f
        private const val FADE_OUT_TIME = 1f
        private const val FULL_END_TIME = 1.5f
        private val EARLY_EXIT_BUTTONS = gdxArrayOf<Any>(
            MegaControllerButton.START,
            MegaControllerButton.SELECT
        )
    }

    private val delayTimer = Timer(DELAY_TIME)
    private val fullEndTimer = Timer(FULL_END_TIME)

    private val fadeOut = Fade(FadeType.FADE_OUT, FADE_OUT_TIME)

    private val activeFonts = Array<MegaFontHandle>()
    private val fontTargets = ObjectMap<MegaFontHandle, Float>()

    private lateinit var creditsQueue: Queue<MegaFontHandle>
    private var creditsComplete = false

    override fun show() {
        GameLogger.debug(TAG, "show()")

        creditsQueue = CreditsLoader.load()

        game.getUiCamera().setToDefaultPosition()
        game.audioMan.playMusic(MusicAsset.VINNYZ_WIP_1_MUSIC, true)

        val blackRegion = game.assMan.getTextureRegion(
            TextureAsset.COLORS.source,
            ConstKeys.BLACK
        )
        fadeOut.setRegion(blackRegion)
        fadeOut.setPosition(0f, 0f)
        fadeOut.setWidth(ConstVals.VIEW_WIDTH * ConstVals.PPM)
        fadeOut.setHeight(ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        fadeOut.init()
    }

    private fun shouldSkipCredits() =
        game.controllerPoller.isAnyJustPressed(EARLY_EXIT_BUTTONS)

    private fun setCreditsComplete() {
        GameLogger.debug(TAG, "setCreditsComplete()")
        creditsComplete = true
        game.audioMan.fadeOutMusic(FADE_OUT_TIME)
    }

    override fun render(delta: Float) {
        if (!creditsComplete && shouldSkipCredits()) {
            GameLogger.debug(TAG, "render(): player triggered early exit")
            game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
            setCreditsComplete()
        }

        if (creditsComplete) {
            fadeOut.update(delta)

            fullEndTimer.update(delta)
            if (fullEndTimer.isFinished()) onCompletion.invoke()

            return
        }

        delayTimer.update(delta)
        if (delayTimer.isFinished() && creditsQueue.notEmpty()) {
            spawnNextFont()
            delayTimer.reset()
        }

        updateCreditsPosition(delta)
    }

    override fun draw(drawer: Batch) {
        drawer.projectionMatrix = game.getUiCamera().combined
        drawer.begin()
        activeFonts.forEach { it.draw(drawer) }
        if (creditsComplete) fadeOut.draw(drawer)
        drawer.end()
    }

    private fun updateCreditsPosition(delta: Float) {
        val iter = activeFonts.iterator()
        while (iter.hasNext()) {
            val font = iter.next()
            font.positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f

            val target = fontTargets[font] + SPEED * delta * ConstVals.PPM
            fontTargets.put(font, target)

            font.positionY = MathUtils.lerp(font.positionY, target, LERP_ALPHA * delta)
            if (font.positionY >= (ConstVals.VIEW_HEIGHT + 1) * ConstVals.PPM) {
                iter.remove()
                fontTargets.remove(font)
            }
        }
        if (!creditsComplete && creditsQueue.isEmpty && activeFonts.isEmpty) {
            GameLogger.debug(TAG, "updateCreditsPosition(): credits queue empty")
            setCreditsComplete()
        }
    }

    override fun reset() {
        GameLogger.debug(TAG, "reset()")
        creditsComplete = false

        fadeOut.reset()

        delayTimer.reset()
        fullEndTimer.reset()

        activeFonts.clear()
        fontTargets.clear()
    }

    private fun spawnNextFont() {
        val nextFont = creditsQueue.removeFirst()
        nextFont.positionY = 0f
        fontTargets.put(nextFont, 0f)
        activeFonts.add(nextFont)
    }
}
