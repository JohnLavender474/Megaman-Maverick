package com.megaman.maverick.game.screens.levels.temp

import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.screens.ScreenEnum

data class LevelDef(val tmxSrcFile: String, val musicAss: MusicAsset, val screenOnCompletion: ScreenEnum)
