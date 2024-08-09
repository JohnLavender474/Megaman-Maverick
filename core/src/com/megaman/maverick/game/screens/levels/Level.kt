package com.megaman.maverick.game.screens.levels

import com.engine.common.extensions.objectMapOf
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.screens.ScreenEnum

enum class Level(tmxSourceFile: String, val musicAss: MusicAsset) {
    // Boss levels
    TIMBER_WOMAN("TimberWoman.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    REACTOR_MAN("ReactMan.tmx", MusicAsset.MM11_FUSE_MAN_MUSIC),
    INFERNO_MAN("InfernoMan_v2.tmx", MusicAsset.MMX6_BLAZE_HEATNIX),
    MOON_MAN("MoonMan_v2.tmx", MusicAsset.MMX5_DARK_DIZZY_MUSIC),

    // Final levels
    WILY_STAGE_1("WilyStage1.tmx", MusicAsset.MMX_SIGMA_1ST_MUSIC),
    WILY_STAGE_2("WilyStage2.tmx", MusicAsset.MMX_SIGMA_2_STAGE),
    WILY_STAGE_3("WilyStage3.tmx", MusicAsset.MMZ_ENEMY_HALL_MUSIC),

    // Bonus levels
    MAGNET_MAN("MagnetMan.tmx", MusicAsset.MM3_MAGNET_MAN_MUSIC),
    DISTRIBUTOR_MAN("Test1.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    ROASTER_MAN("Test1.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    BLUNT_MAN("Test1.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    NUKE_MAN("Test1.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    CREW_MAN("CrewMan.tmx", MusicAsset.MM7_JUNK_MAN_MUSIC),
    FREEZE_MAN("FreezeMan.tmx", MusicAsset.MMX_CHILL_PENGUIN_MUSIC),
    MICROWAVE_MAN("Test1.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    GALAXY_MAN("GalaxyMan.tmx", MusicAsset.MM9_GALAXY_MAN_MUSIC),

    // Test levels
    TEST1("Test1.tmx", MusicAsset.MMX2_X_HUNTER_MUSIC),
    TEST2("Test2.tmx", MusicAsset.MMX5_VOLT_KRAKEN_MUSIC),
    TEST3("Test3.tmx", MusicAsset.XENOBLADE_GAUR_PLAINS_MUSIC),
    TEST4("Test4.tmx", MusicAsset.MMX_SIGMA_1ST_MUSIC),
    TEST5("Test5.tmx", MusicAsset.MMZ_NEO_ARCADIA_MUSIC),
    TEST6("Test6.tmx", MusicAsset.MMX3_INTRO_STAGE_MUSIC),
    TEST7("Test7.tmx", MusicAsset.MMX_SIGMA_2_STAGE);

    val tmxSourceFile = "tiled_maps/tmx/$tmxSourceFile"
}

object LevelCompletionMap {

    private val map = objectMapOf(
        Level.MAGNET_MAN to ScreenEnum.SAVE_GAME_SCREEN,
        Level.TIMBER_WOMAN to ScreenEnum.SAVE_GAME_SCREEN,
        Level.DISTRIBUTOR_MAN to ScreenEnum.SAVE_GAME_SCREEN,
        Level.ROASTER_MAN to ScreenEnum.SAVE_GAME_SCREEN,
        Level.INFERNO_MAN to ScreenEnum.SAVE_GAME_SCREEN,
        Level.BLUNT_MAN to ScreenEnum.SAVE_GAME_SCREEN,
        Level.NUKE_MAN to ScreenEnum.SAVE_GAME_SCREEN,
        Level.CREW_MAN to ScreenEnum.SAVE_GAME_SCREEN,
        Level.FREEZE_MAN to ScreenEnum.SAVE_GAME_SCREEN,
        Level.MICROWAVE_MAN to ScreenEnum.SAVE_GAME_SCREEN,
        Level.GALAXY_MAN to ScreenEnum.SAVE_GAME_SCREEN,
        Level.WILY_STAGE_1 to ScreenEnum.WILY_CASTLE_SCREEN,
        Level.WILY_STAGE_2 to ScreenEnum.WILY_CASTLE_SCREEN,
        Level.WILY_STAGE_3 to ScreenEnum.WILY_CASTLE_SCREEN
    )

    fun getNextScreen(level: Level): ScreenEnum = map[level]
}
