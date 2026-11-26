package com.megaman.maverick.game.screens.menus.title

import com.badlogic.gdx.graphics.g2d.TextureRegion

class TitleLetter(
    val name: String,
    val homeX: Int,
    val homeY: Int,
    val startStep: Int,
    val beamColor: Int,
    val region: TextureRegion,
    val whiteRegion: TextureRegion
) {

    val width = region.regionWidth
    val height = region.regionHeight
    val centerColumn = width / 2
    val bottomRow = homeY + height
    val scratch = TextureRegion(region)
}
