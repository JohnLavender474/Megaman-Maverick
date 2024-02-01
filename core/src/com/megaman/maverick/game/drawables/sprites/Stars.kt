package com.megaman.maverick.game.drawables.sprites

import com.engine.common.extensions.getTextureRegion
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

class Stars(game: MegamanMaverickGame, var startX: Float, var startY: Float) :
    Background(
        startX,
        startY,
        game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_1.source, "StarFieldBG"),
        DrawingPriority(DrawingSection.FOREGROUND, 1),
        WIDTH * ConstVals.PPM,
        HEIGHT * ConstVals.PPM,
        ROWS,
        COLS
    ) {

    companion object {
        private const val ROWS = 1
        private const val COLS = 6
        private const val DUR = 10f
        private const val WIDTH = ConstVals.VIEW_WIDTH / 3f
        private const val HEIGHT = ConstVals.VIEW_HEIGHT / 4f
    }

    private var dist = 0f

    override fun update(delta: Float) {
        super.update(delta)
        val trans = WIDTH * ConstVals.PPM * delta / DUR
        backgroundSprites.translate(-trans, 0f)
        dist += trans
        if (dist >= WIDTH * ConstVals.PPM) {
            backgroundSprites.setPosition(startX, startY)
            dist = 0f
        }
    }
}
