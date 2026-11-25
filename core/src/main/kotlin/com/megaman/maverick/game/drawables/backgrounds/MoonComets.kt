package com.megaman.maverick.game.drawables.backgrounds

import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

/** Comets, meteor streaks and small rocks (Moon Man): a 32x14 tile view, tiled in both directions. */
class MoonComets(game: MegamanMaverickGame, it: RectangleMapObject) : Background(
    game = game,
    key = TAG,
    startX = it.rectangle.x,
    startY = it.rectangle.y,
    model = game.assets.getTextureRegion(TextureAsset.BACKGROUNDS_14.source, TAG),
    modelWidth = 32f * ConstVals.PPM,
    modelHeight = 14f * ConstVals.PPM,
    rows = 3,
    columns = 4,
    priority = DrawingPriority(DrawingSection.BACKGROUND, 3),
    parallaxX = 0.12f,
    parallaxY = 0.08f
) {

    companion object {
        const val TAG = "MoonComets"
    }
}
