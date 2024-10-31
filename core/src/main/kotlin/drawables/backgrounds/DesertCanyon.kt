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

class DesertCanyon(assMan: AssetManager, it: RectangleMapObject) : Background(
    "DesertCanyon",
    it.rectangle.x,
    it.rectangle.y,
    assMan.getTextureRegion(TextureAsset.BACKGROUNDS_6.source, "Desert/Canyon"),
    it.rectangle.width,
    it.rectangle.height,
    rows = 1,
    columns = 100,
    parallaxY = 0f,
    parallaxX = 0.1f,
    priority = DrawingPriority(DrawingSection.BACKGROUND, 1),
    initPos = Vector2(it.rectangle.getCenter().x + 0.5f * ConstVals.PPM, it.rectangle.getCenter().y)
)
