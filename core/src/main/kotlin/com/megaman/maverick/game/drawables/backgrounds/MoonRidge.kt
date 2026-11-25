package com.megaman.maverick.game.drawables.backgrounds

import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

/** The near crater ridge at the bottom of the view (Moon Man): a 32x5 tile strip, repeated in x only. */
class MoonRidge(game: MegamanMaverickGame, it: RectangleMapObject) : Background(
    game = game,
    key = TAG,
    startX = it.rectangle.x,
    startY = it.rectangle.y,
    model = game.assets.getTextureRegion(TextureAsset.BACKGROUNDS_14.source, TAG),
    modelWidth = 32f * ConstVals.PPM,
    modelHeight = 5f * ConstVals.PPM,
    rows = 1,
    columns = 8,
    priority = DrawingPriority(DrawingSection.BACKGROUND, 4),
    parallaxX = 0.25f,
    parallaxY = 0f
) {

    companion object {
        const val TAG = "MoonRidge"
    }
}
