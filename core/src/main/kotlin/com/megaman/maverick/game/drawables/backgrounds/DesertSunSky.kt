package com.megaman.maverick.game.drawables.backgrounds

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

class DesertSunSky(game: MegamanMaverickGame, it: RectangleMapObject) : AnimatedBackground(
    game,
    "DesertSunSky",
    startX = it.rectangle.x,
    startY = it.rectangle.y,
    model = game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_6.source, "Desert/SunSky"),
    modelWidth = 16f * ConstVals.PPM,
    modelHeight = 14f * ConstVals.PPM,
    rows = 1,
    columns = 1,
    animRows = 3,
    animColumns = 1,
    duration = 0.1f,
    priority = DrawingPriority(DrawingSection.BACKGROUND, 0),
    parallaxX = 0f,
    parallaxY = 0f
)
