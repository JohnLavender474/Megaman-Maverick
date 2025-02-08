package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.isAnyKeyJustPressed
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

class SimpleInitGameScreen(private val game: MegamanMaverickGame) : BaseScreen(), Initializable {

    companion object {
        const val TAG = "SimpleInitGameScreen"
        private val START_BUTTONS =
            gdxArrayOf<Any>(MegaControllerButton.START, MegaControllerButton.A, MegaControllerButton.B)
    }

    private lateinit var startGameText: MegaFontHandle
    private val uiCamera = game.getUiCamera()
    private var initialized = false

    override fun init() {
        if (initialized) {
            GameLogger.debug(TAG, "init(): already initialized, do nothing")
            return
        }

        initialized = true

        startGameText = MegaFontHandle(
            "PRESS ENTER TO START GAME",
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
        )

        GameLogger.debug(TAG, "init(): initialized")
    }

    override fun show() {
        GameLogger.debug(TAG, "show()")
        if (!initialized) init()
        super.show()
        uiCamera.setToDefaultPosition()
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) Gdx.app.exit()

        super.render(delta)

        game.batch.projectionMatrix = uiCamera.combined
        game.batch.begin()
        startGameText.draw(game.batch)
        game.batch.end()

        if (Gdx.input.isAnyKeyJustPressed(Keys.SPACE, Keys.ENTER, Keys.J, Keys.K) ||
            game.controllerPoller.isAnyJustReleased(START_BUTTONS)
        ) {
            GameLogger.debug(TAG, "render(): set to next screen")
            game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
            game.setCurrentScreen(ScreenEnum.LOGO_SCREEN.name)
            // game.startLevelScreen(LevelDefinition.INFERNO_MAN)
        }
    }
}
