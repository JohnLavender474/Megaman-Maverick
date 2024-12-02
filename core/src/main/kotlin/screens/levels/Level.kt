package com.megaman.maverick.game.screens.levels

import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.pairTo
import com.megaman.maverick.game.StartScreenOption
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.screens.ScreenEnum

enum class Level(tmxSourceFile: String, val musicAss: MusicAsset) {
    // Boss levels
    DESERT_MAN("DesertMan_16x12.tmx", MusicAsset.MMX7_VANISHING_GUNGAROO_MUSIC),
    GLACIER_MAN("GlacierMan_16x12.tmx", MusicAsset.MM8_FROST_MAN_ALT_MUSIC),
    REACTOR_MAN("ReactorMan_16x12.tmx", MusicAsset.MMX8_BURN_ROOSTER_MUSIC),
    TIMBER_WOMAN("TimberWoman.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    INFERNO_MAN("InfernoMan_16x12.tmx", MusicAsset.MMX6_BLAZE_HEATNIX_MUSIC),
    MOON_MAN("MoonMan_16x12.tmx", MusicAsset.MMX5_DARK_DIZZY_MUSIC),
    RODENT_MAN("RodentMan.tmx", MusicAsset.MM7_SLASH_MAN_MUSIC),

    // Final levels
    WILY_STAGE_1("WilyStage1.tmx", MusicAsset.MMX6_GATE_STAGE_MUSIC),
    WILY_STAGE_2("WilyStage2.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    WILY_STAGE_3("WilyStage3_v2.tmx", MusicAsset.MMX6_GATE_STAGE_MUSIC),

    // Bonus levels
    DISTRIBUTOR_MAN("Test1.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    ROASTER_MAN("Test1.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    BLUNT_MAN("Test1.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    MAGNET_MAN("MagnetMan.tmx", MusicAsset.MMX5_IZZY_GLOW_MUSIC),
    NUKE_MAN("Test1.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    CREW_MAN("CrewMan.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    FREEZE_MAN("FreezeMan.tmx", MusicAsset.MMX_CHILL_PENGUIN_MUSIC),
    MICROWAVE_MAN("Test1.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    GALAXY_MAN("GalaxyMan.tmx", MusicAsset.MM9_GALAXY_MAN_MUSIC),
    NAVAL_MAN("NavalMan.tmx", MusicAsset.MMX_CHILL_PENGUIN_MUSIC),

    // Test levels
    TEST1("Test1.tmx", MusicAsset.MMX2_X_HUNTER_MUSIC),
    TEST2("Test2.tmx", MusicAsset.MMX5_VOLT_KRAKEN_MUSIC),
    TEST3("Test3.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    TEST4("Test4.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    TEST5("Test5.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    TEST6("Test6.tmx", MusicAsset.MMX3_INTRO_STAGE_MUSIC),
    TEST7("Test7.tmx", MusicAsset.MM3_SNAKE_MAN_MUSIC),
    TEST8("Test8.tmx", MusicAsset.MMX5_VOLT_KRAKEN_MUSIC);

    val tmxSourceFile = "tiled_maps/tmx/$tmxSourceFile"
}

object LevelCompletionMap {

    private val prodMap = objectMapOf(
        Level.MAGNET_MAN pairTo ScreenEnum.SAVE_GAME_SCREEN,
        Level.TIMBER_WOMAN pairTo ScreenEnum.SAVE_GAME_SCREEN,
        Level.DISTRIBUTOR_MAN pairTo ScreenEnum.SAVE_GAME_SCREEN,
        Level.ROASTER_MAN pairTo ScreenEnum.SAVE_GAME_SCREEN,
        Level.INFERNO_MAN pairTo ScreenEnum.SAVE_GAME_SCREEN,
        Level.BLUNT_MAN pairTo ScreenEnum.SAVE_GAME_SCREEN,
        Level.NUKE_MAN pairTo ScreenEnum.SAVE_GAME_SCREEN,
        Level.CREW_MAN pairTo ScreenEnum.SAVE_GAME_SCREEN,
        Level.FREEZE_MAN pairTo ScreenEnum.SAVE_GAME_SCREEN,
        Level.MICROWAVE_MAN pairTo ScreenEnum.SAVE_GAME_SCREEN,
        Level.GALAXY_MAN pairTo ScreenEnum.SAVE_GAME_SCREEN,
        Level.WILY_STAGE_1 pairTo ScreenEnum.WILY_CASTLE_SCREEN,
        Level.WILY_STAGE_2 pairTo ScreenEnum.WILY_CASTLE_SCREEN,
        Level.WILY_STAGE_3 pairTo ScreenEnum.WILY_CASTLE_SCREEN
    )

    fun getNextScreen(level: Level, gameType: StartScreenOption = StartScreenOption.MAIN): ScreenEnum =
        when (gameType) {
            StartScreenOption.MAIN -> prodMap[level]
            StartScreenOption.SIMPLE -> ScreenEnum.SIMPLE_INIT_GAME_SCREEN
            StartScreenOption.LEVEL -> throw IllegalStateException(
                "Cannot continue after level has completed when playing in \"level\" mode"
            )
        }
}
