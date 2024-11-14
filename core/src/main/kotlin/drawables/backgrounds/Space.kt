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

class Space(assMan: AssetManager, it: RectangleMapObject) : AnimatedBackground(
    TAG,
    startX = it.rectangle.x,
    startY = it.rectangle.y,
    modelWidth = MODEL_WIDTH * ConstVals.PPM,
    modelHeight = MODEL_HEIGHT * ConstVals.PPM,
    rows = ROWS,
    columns = COLS,
    parallaxX = 0.1f,
    parallaxY = 0.1f,
    animRows = ANIM_ROWS,
    animColumns = ANIM_COLS,
    duration = ANIM_DUR,
    model = assMan.getTextureRegion(TextureAsset.BACKGROUNDS_6.source, TAG),
    priority = DrawingPriority(DrawingSection.BACKGROUND, 0),
    initPos = Vector2(it.rectangle.getCenter().x + 15f * ConstVals.PPM, it.rectangle.getCenter().y + 5f * ConstVals.PPM)
) {

    companion object {
        const val TAG = "Space"
        private const val ROWS = 100
        private const val COLS = 100
        private const val ANIM_ROWS = 2
        private const val ANIM_COLS = 1
        private const val ANIM_DUR = 0.25f
        private const val MODEL_WIDTH = 8f
        private const val MODEL_HEIGHT = 7f
    }
}
