package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.shapes.getCenter
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset

class UndergroundPipes(assMan: AssetManager, it: RectangleMapObject) : Background(
    key = TAG,
    startX = it.rectangle.x,
    startY = it.rectangle.y,
    model = assMan.getTextureRegion(TextureAsset.BACKGROUNDS_6.source, TAG),
    modelWidth = WIDTH * ConstVals.PPM,
    modelHeight = HEIGHT * ConstVals.PPM,
    rows = 1,
    columns = 100,
    parallaxY = 0f,
    parallaxX = 0.15f,
    priority = DrawingPriority(DrawingSection.BACKGROUND, 1),
    initPos = Vector2(
        it.rectangle.getCenter().x + 10f * ConstVals.PPM,
        it.rectangle.getCenter().y + 7.5f * ConstVals.PPM
    )
) {

    companion object {
        const val TAG = "UndergroundPipes"
        const val WIDTH = 60f
        const val HEIGHT = 15f
        const val ALPHA = 0.5f
    }

    init {
        backgroundSprites.forEach { it.setAlpha(ALPHA) }
    }
}
