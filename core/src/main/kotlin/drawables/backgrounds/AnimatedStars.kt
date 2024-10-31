package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.getTextureRegion
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

class AnimatedStars(
    game: MegamanMaverickGame,
    start: Vector2,
    width: Float = MODEL_WIDTH * ConstVals.PPM,
    height: Float = MODEL_HEIGHT * ConstVals.PPM
) : AnimatedBackground(
    "AnimatedStars",
    start.x - (ROWS * ConstVals.PPM) / 2f,
    start.y - (COLS * ConstVals.PPM) / 2f,
    game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_1.source, "AnimatedStarsBG"),
    width,
    height,
    ROWS,
    COLS,
    ANIM_ROWS,
    ANIM_COLS,
    ANIM_DUR,
    initPos = Vector2(start.x + (ROWS * ConstVals.PPM) / 2f, start.y + (COLS * ConstVals.PPM) / 2f)
) {

    companion object {
        private const val ROWS = 100
        private const val COLS = 100
        private const val ANIM_ROWS = 1
        private const val ANIM_COLS = 3
        private const val ANIM_DUR = 0.5f
        private const val MODEL_WIDTH = ConstVals.VIEW_WIDTH / 3f
        private const val MODEL_HEIGHT = ConstVals.VIEW_HEIGHT / 4f
    }
}
