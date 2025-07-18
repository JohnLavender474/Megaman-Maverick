package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.getTextureRegion
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

class ScrollingStars(
    game: MegamanMaverickGame,
    start: Vector2,
    target: Vector2 = start.cpy().add(WIDTH * ConstVals.PPM, 0f),
    width: Float = WIDTH * ConstVals.PPM,
    height: Float = HEIGHT * ConstVals.PPM
) : ScrollingBackground(
    game,
    "ScrollingStars",
    game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_1.source, "StarFieldBG"),
    start,
    target,
    DUR,
    width,
    height,
    ROWS,
    COLS,
    initPos = start.cpy()
) {

    companion object {
        const val ROWS = 8
        const val COLS = 8
        const val DUR = 10f
        const val WIDTH = 16f
        const val HEIGHT = 14f
    }
}
