package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.extensions.getCenter

class EarthBackdrop(assMan: AssetManager, it: RectangleMapObject) : Background(
    TAG,
    startX = it.rectangle.x,
    startY = it.rectangle.y,
    modelWidth = it.rectangle.width,
    modelHeight = it.rectangle.height,
    rows = ROWS,
    columns = COLS,
    parallaxX = 0.25f,
    parallaxY = 0.1f,
    model = assMan.getTextureRegion(TextureAsset.BACKGROUNDS_6.source, TAG),
    priority = DrawingPriority(DrawingSection.BACKGROUND, 1),
    initPos = Vector2(it.rectangle.getCenter().x, it.rectangle.getCenter().y + 3f * ConstVals.PPM)
) {

    companion object {
        const val TAG = "EarthBackdrop"
        private const val ROWS = 1
        private const val COLS = 1
    }
}

