package com.megaman.lostmemories

import com.badlogic.gdx.utils.ObjectMap
import com.megaman.lostmemories.screens.LostMemoriesSplashScreen
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.titles.IGameTitle

object LostMemoriesTitle : IGameTitle {

    const val LOST_MEMORIES_SPLASH = "LOST_MEMORIES_SPLASH"
    const val SAVE_FOLDER_NAME = ".mega-man-x-lost-memories"
    const val PLAYER_SETTINGS_FILE_NAME = "Mega Man X Lost Memories - Player Settings"

    override val saveFolderName = SAVE_FOLDER_NAME

    override val playerSettingsFileName = PLAYER_SETTINGS_FILE_NAME

    private val redirects = ObjectMap<String, String>().apply {
        put(ScreenEnum.SAVE_GAME_SCREEN.name, LOST_MEMORIES_SPLASH)
        put(ScreenEnum.EXTRAS_SCREEN.name, LOST_MEMORIES_SPLASH)
        put(ScreenEnum.ENDING_SCREEN.name, LOST_MEMORIES_SPLASH)
        put(ScreenEnum.MAIN_MENU_SCREEN.name, LOST_MEMORIES_SPLASH)
    }

    override fun registerScreens(game: MegamanMaverickGame) {
        game.screens.put(LOST_MEMORIES_SPLASH, LostMemoriesSplashScreen(game))
    }

    override fun openDefaultStart(game: MegamanMaverickGame) {
        game.setCurrentScreen(LOST_MEMORIES_SPLASH)
    }

    override fun redirectScreen(key: String): String? = redirects.get(key)
}
