package com.megaman.maverick.game.drawables.sprites

import com.badlogic.gdx.math.Vector2
import com.engine.common.extensions.getTextureRegion
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

class ScrollingStars(
    game: MegamanMaverickGame, start: Vector2, target: Vector2 = start.cpy().add(WIDTH * ConstVals.PPM, 0f),
    width: Float = WIDTH * ConstVals.PPM, height: Float = HEIGHT * ConstVals.PPM
) : ScrollingBackground(
        game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_1.source, "StarFieldBG"),
        start, target, DUR, width, height, ROWS, COLS
    ) {

    companion object {
        private const val ROWS = 1
        private const val COLS = 6
        private const val DUR = 10f
        private const val WIDTH = ConstVals.VIEW_WIDTH / 3f
        private const val HEIGHT = ConstVals.VIEW_HEIGHT / 4f
    }
}
