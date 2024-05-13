package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.engine.drawables.fonts.BitmapFontHandle
import com.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.setToDefaultPosition

class SimpleInitGameScreen(private val megamanGame: MegamanMaverickGame) : BaseScreen(megamanGame) {

    override val eventKeyMask = ObjectSet<Any>()

    private lateinit var text: BitmapFontHandle
    private val uiCamera = megamanGame.getUiCamera()

    override fun init() {
        text =
            BitmapFontHandle(
                { "Press enter to start game" },
                MegaUtilMethods.getDefaultFontSize(),
                Vector2(
                    ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
                    ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
                ),
                centerX = true,
                centerY = true,
                fontSource = ConstVals.MEGAMAN_MAVERICK_FONT
            )
    }

    override fun show() {
        super.show()
        uiCamera.setToDefaultPosition()
    }

    override fun render(delta: Float) {
        super.render(delta)
        game.batch.projectionMatrix = uiCamera.combined
        game.batch.begin()
        text.draw(game.batch)
        game.batch.end()
        if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
            megamanGame.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
            megamanGame.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
        }
    }
}
