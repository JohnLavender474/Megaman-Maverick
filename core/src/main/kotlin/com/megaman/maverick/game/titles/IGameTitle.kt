package com.megaman.maverick.game.titles

import com.megaman.maverick.game.MegamanMaverickGame

interface IGameTitle {

    val saveFileName: String

    val playerSettingsFileName: String

    fun registerScreens(game: MegamanMaverickGame)

    fun openDefaultStart(game: MegamanMaverickGame)

    fun redirectScreen(key: String): String?
}
