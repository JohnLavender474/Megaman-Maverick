package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.getTextureRegion
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.sprites.ScrollingBackground

class ScrollingStars(
    game: MegamanMaverickGame,
    start: Vector2,
    target: Vector2 = start.cpy().add(WIDTH * ConstVals.PPM, 0f),
    width: Float = WIDTH * ConstVals.PPM,
    height: Float = HEIGHT * ConstVals.PPM
) : ScrollingBackground(
    "ScrollingStars",
    game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_1.source, "StarFieldBG"),
    start,
    target,
    DUR,
    width,
    height,
    ROWS,
    COLS,
    initPos = start
) {

    companion object {
        const val ROWS = 8
        const val COLS = 8
        const val DUR = 10f
        const val WIDTH = 5f
        const val HEIGHT = 3f
    }
}
