package com.megaman.maverick.game.screens.other

import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle

class SimpleEndLevelScreen(private val game: MegamanMaverickGame) : BaseScreen() {

    private lateinit var successText: MegaFontHandle

    override fun show() {
        super.show()
        successText =
            MegaFontHandle(
                text = "LEVEL COMPLETE!",
                positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
                positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f,
                centerX = true,
                centerY = true
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
