package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

class GameOverScreen(private val game: MegamanMaverickGame): BaseScreen(), Initializable {

    companion object {
        const val TAG = "GameOverScreen"
        private const val GAME_OVER = "GAME OVER"
        private const val PRESS_START = "PRESS START"
        private const val PRESS_START_BLINK = 0.25f
    }

    private val fontHandles = OrderedMap<String, MegaFontHandle>()

    private val pressStartBlinkTimer = Timer(PRESS_START_BLINK)
    private var pressStartBlink = true

    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        val gameOver = MegaFontHandle(
            text = GAME_OVER,
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f,
            centerX = true,
            centerY = true
        )
        fontHandles.put(GAME_OVER, gameOver)

        val pressStart = MegaFontHandle(
            text = PRESS_START,
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = ((ConstVals.VIEW_HEIGHT / 2) - 1) * ConstVals.PPM,
            centerX = true,
            centerY = true
        )
        fontHandles.put(PRESS_START, pressStart)
    }

    override fun show() {
        if (!initialized) init()
        super.show()

        game.getUiCamera().setToDefaultPosition()

        pressStartBlinkTimer.reset()
        pressStartBlink = true

        game.audioMan.playMusic(MusicAsset.MM3_GAME_OVER_MUSIC, true)
    }

    override fun render(delta: Float) {
        super.render(delta)

        pressStartBlinkTimer.update(delta)
        if (pressStartBlinkTimer.isFinished()) {
            pressStartBlink = !pressStartBlink
            pressStartBlinkTimer.reset()
        }

        if (game.controllerPoller.isJustReleased(MegaControllerButton.START))
            game.setCurrentScreen(ScreenEnum.SAVE_GAME_SCREEN.name)
    }

    override fun draw(drawer: Batch) {
        super.draw(drawer)

        val batch = game.batch
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()

        fontHandles[GAME_OVER].draw(drawer)
        if (pressStartBlink) fontHandles[PRESS_START].draw(drawer)

        batch.end()
    }
}
