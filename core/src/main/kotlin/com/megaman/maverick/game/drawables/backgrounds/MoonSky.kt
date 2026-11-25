package com.megaman.maverick.game.drawables.backgrounds

import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

/** The big moon, a small moon and a ringed planet (Moon Man): one 32x14 tile strip, repeated in x only. */
class MoonSky(game: MegamanMaverickGame, it: RectangleMapObject) : Background(
    game = game,
    key = TAG,
    startX = it.rectangle.x,
    startY = it.rectangle.y,
    model = game.assets.getTextureRegion(TextureAsset.BACKGROUNDS_14.source, TAG),
    modelWidth = 32f * ConstVals.PPM,
    modelHeight = 14f * ConstVals.PPM,
    rows = 1,
    columns = 3,
    priority = DrawingPriority(DrawingSection.BACKGROUND, 2),
    parallaxX = 0.05f,
    parallaxY = 0f
) {

    companion object {
        const val TAG = "MoonSky"
    }
}
