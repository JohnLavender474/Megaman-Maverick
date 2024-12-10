package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

object CreditsLoader {

    const val CREDITS_SOURCE = "credits.txt"

    fun load(): Queue<MegaFontHandle> {
        val credits = Queue<MegaFontHandle>()
        val inputStream = InternalFileHandleResolver().resolve(CREDITS_SOURCE).read()
        val reader = inputStream.reader()
        reader.forEachLine { line ->
            val fontSize = when {
                line.startsWith("#") -> MegaUtilMethods.getLargeFontSize()
                line.startsWith("##") -> MegaUtilMethods.getDefaultFontSize()
                else -> MegaUtilMethods.getSmallFontSize()
            }
            credits.addLast(
                MegaFontHandle(
                    text = line.replace("#", "").uppercase(),
                    fontSize = fontSize,
                    fontSource = ConstVals.MEGAMAN_MAVERICK_FONT
                )
            )
        }
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
        private const val SPEED = 1.5f
        private const val FADE_OUT_TIME = 2f
    }

    private val delayTimer = Timer(DELAY_TIME)
    private val fadeOutTimer = Timer(FADE_OUT_TIME)
    private val activeFonts = Array<MegaFontHandle>()
    private lateinit var creditsQueue: Queue<MegaFontHandle>
    private var creditsComplete = false

    override fun show() {
        creditsQueue = CreditsLoader.load()
        delayTimer.reset()
        game.audioMan.playMusic(MusicAsset.MM2_CREDITS_MUSIC, false)
        game.getUiCamera().setToDefaultPosition()
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (game.paused) game.pause()
            else game.resume()
        }
        if (game.paused) return

        if (creditsComplete) {
            handleFadeout(delta)
            return
        }

        delayTimer.update(delta)
        if (delayTimer.isFinished() && creditsQueue.notEmpty()) {
            spawnNextFont()
            delayTimer.reset()
        }

        updateCreditsPosition(delta)

        game.batch.projectionMatrix = game.getUiCamera().combined
        game.batch.begin()
        activeFonts.forEach { it.draw(game.batch) }
        game.batch.end()
    }

    private fun updateCreditsPosition(delta: Float) {
        val iter = activeFonts.iterator()
        while (iter.hasNext()) {
            val font = iter.next()
            font.positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f
            font.positionY += SPEED * delta * ConstVals.PPM
            if (font.positionY >= (ConstVals.VIEW_HEIGHT + 1) * ConstVals.PPM) iter.remove()
        }
        if (creditsQueue.isEmpty && activeFonts.isEmpty) {
            creditsComplete = true
            fadeOutTimer.reset()
            game.audioMan.fadeOutMusic(FADE_OUT_TIME)
        }
    }

    private fun spawnNextFont() {
        val nextFont = creditsQueue.removeFirst()
        nextFont.positionY = 0f
        activeFonts.add(nextFont)
    }

    private fun handleFadeout(delta: Float) {
        fadeOutTimer.update(delta)
        // TODO: update alpha of background images
        if (fadeOutTimer.isFinished()) onCompletion.invoke()
    }
}
