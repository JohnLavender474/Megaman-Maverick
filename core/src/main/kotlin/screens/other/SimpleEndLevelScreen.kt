package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.drawables.fonts.BitmapFontHandle
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize

class SimpleEndLevelScreen(private val game: MegamanMaverickGame) : BaseScreen() {

    private lateinit var successText: BitmapFontHandle

    override fun show() {
        super.show()
        successText =
            BitmapFontHandle(
                "LEVEL COMPLETE!",
                getDefaultFontSize(),
                Vector2(
                    ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
                    ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
                ),
                centerX = true,
                centerY = true,
                fontSource = ConstVals.MEGAMAN_MAVERICK_FONT
            )
        game.audioMan.playSound(SoundAsset.MM2_VICTORY_SOUND, false)
    }

    override fun render(delta: Float) {
        super.render(delta)
        game.batch.projectionMatrix = game.getUiCamera().combined
        game.batch.begin()
        successText.draw(game.batch)
        game.batch.end()
    }
}
