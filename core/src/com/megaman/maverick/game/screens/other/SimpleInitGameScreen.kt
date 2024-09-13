package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.menus.ControllerSettingsScreen
import com.megaman.maverick.game.utils.setToDefaultPosition

class SimpleInitGameScreen(private val game: MegamanMaverickGame) : BaseScreen(), Initializable {

    private lateinit var startGameText: MegaFontHandle
    private lateinit var keyboardSettingsText: MegaFontHandle
    private lateinit var controllerSettingsText: MegaFontHandle
    private lateinit var controllerWarningText: MegaFontHandle
    private lateinit var creditsScreenText: MegaFontHandle
    private val uiCamera = game.getUiCamera()
    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        startGameText = MegaFontHandle(
            { "Press enter to start game" },
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = (ConstVals.VIEW_HEIGHT - 2f) * ConstVals.PPM
        )
        keyboardSettingsText = MegaFontHandle(
            { "Press k to configure keyboard" },
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = (ConstVals.VIEW_HEIGHT - 3.5f) * ConstVals.PPM,
        )
        controllerSettingsText = MegaFontHandle(
            { "Press c to configure controller" },
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = (ConstVals.VIEW_HEIGHT - 5f) * ConstVals.PPM,
        )
        controllerWarningText = MegaFontHandle(
            { "(Controller settings aborts\nif no controller connected)" },
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = (ConstVals.VIEW_HEIGHT - 6.5f) * ConstVals.PPM
        )
        creditsScreenText = MegaFontHandle(
            { "Press a to view credits" },
            positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = (ConstVals.VIEW_HEIGHT - 10f) * ConstVals.PPM
        )
    }

    override fun show() {
        if (!initialized) init()
        super.show()
        uiCamera.setToDefaultPosition()

        // showing this screen means we're in 'simple' mode, so configure screens accordingly

        val creditsScreen = game.screens[ScreenEnum.CREDITS_SCREEN.name] as CreditsScreen
        creditsScreen.onCompletion = { game.setCurrentScreen(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name) }

        val backAction = {
            game.setCurrentScreen(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name)
            true
        }
        val keyboardSettingsScreen =
            game.screens[ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name] as ControllerSettingsScreen
        keyboardSettingsScreen.backAction = backAction
        val controllerSettingsScreen =
            game.screens[ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name] as ControllerSettingsScreen
        controllerSettingsScreen.backAction = backAction
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) Gdx.app.exit()
        super.render(delta)
        game.batch.projectionMatrix = uiCamera.combined
        game.batch.begin()
        startGameText.draw(game.batch)
        keyboardSettingsText.draw(game.batch)
        controllerSettingsText.draw(game.batch)
        controllerWarningText.draw(game.batch)
        creditsScreenText.draw(game.batch)
        game.batch.end()
        if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
            game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
            game.setCurrentScreen(ScreenEnum.SIMPLE_SELECT_LEVEL_SCREEN.name)
        } else if (Gdx.input.isKeyJustPressed(Keys.K)) {
            game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
            game.setCurrentScreen(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name)
        } else if (Gdx.input.isKeyJustPressed(Keys.C)) {
            game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
            game.setCurrentScreen(ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name)
        } else if (Gdx.input.isKeyJustPressed(Keys.A)) {
            game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
            game.setCurrentScreen(ScreenEnum.CREDITS_SCREEN.name)
        }
    }
}
