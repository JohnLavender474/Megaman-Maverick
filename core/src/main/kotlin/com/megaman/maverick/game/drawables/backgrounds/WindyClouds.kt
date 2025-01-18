package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.getTextureRegion
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

class WindyClouds(
    game: MegamanMaverickGame, start: Vector2, width: Float, height: Float
) : ScrollingBackground(
    "WindyClouds",
    game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_2.source, "BKG04"),
    start,
    start.cpy().sub(width, 0f),
    DUR,
    width,
    height,
    ROWS,
    COLS,
    initPos = Vector2(start).add((width / 2f) + 2f * ConstVals.PPM, height / 2f)
) {

    companion object {
        private const val ROWS = 1
        private const val COLS = 30
        private const val DUR = 10f
    }
}
