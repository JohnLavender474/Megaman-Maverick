package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.extensions.getCenter

class DesertSunSky(assMan: AssetManager, it: RectangleMapObject) : AnimatedBackground(
    "DesertSunSky",
    startX = it.rectangle.x,
    startY = it.rectangle.y,
    model = assMan.getTextureRegion(TextureAsset.BACKGROUNDS_6.source, "Desert/SunSky"),
    modelWidth = it.rectangle.width,
    modelHeight = it.rectangle.height,
    rows = 1,
    columns = 1,
    animRows = 3,
    animColumns = 1,
    duration = 0.1f,
    priority = DrawingPriority(DrawingSection.BACKGROUND, 0),
    parallaxX = 0f,
    parallaxY = 0f,
    initPos = Vector2(it.rectangle.getCenter().x, it.rectangle.getCenter().y - 0.5f * ConstVals.PPM)
)
