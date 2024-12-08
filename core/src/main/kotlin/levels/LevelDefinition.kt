package com.megaman.maverick.game.levels

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.screens.ScreenEnum

data class LevelDefinition(
    var type: LevelType,
    var music: MusicAsset,
    var tmxMapSource: String,
    var mugshotRegion: TextureRegion,
    var screenOnCompletion: ScreenEnum,
)
