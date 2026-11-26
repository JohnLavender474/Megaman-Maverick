package com.megaman.maverick.game.titles

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.MegamanMaverickGame.RunType
import com.megaman.maverick.game.PreferenceFiles
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.screens.ScreenEnum

object MaverickTitle : IGameTitle {

    override val saveFolderName = ".megaman-maverick"

    override val playerSettingsFileName = PreferenceFiles.MEGAMAN_MAVERICK_PLAYER_SETTINGS

    override fun registerScreens(game: MegamanMaverickGame) {}

    override fun openDefaultStart(game: MegamanMaverickGame) {
        when (game.params.runType) {
            RunType.GAME -> game.setCurrentScreen(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name)
            RunType.TEST -> game.startLevel(LevelDefinition.TEST_1)
        }
    }

    override fun redirectScreen(key: String): String? = null
}
