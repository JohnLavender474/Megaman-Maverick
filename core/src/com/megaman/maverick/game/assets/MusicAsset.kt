package com.megaman.maverick.game.assets

import com.badlogic.gdx.audio.Music

const val MUSIC_ASSET_PREFIX = "music/"

enum class MusicAsset(src: String) : IAsset {
    DUEL_OF_THE_FATES_MUSIC("duel_of_the_fates.mp3"),
    XENOBLADE_GAUR_PLAINS_MUSIC("xenoblade_gaur_plains.ogg"),
    MM11_WILY_STAGE_MUSIC("MM11_Wily_Stage.mp3"),
    FF7_BOSS_MUSIC("FF7_LetTheBattlesBegin.mp3"),
    MM11_FUSE_MAN_MUSIC("Megaman11_FuseMan.mp3"),
    MM7_JUNK_MAN_MUSIC("MM7_JunkMan.mp3"),
    MM11_TORCH_MAN_MUSIC("Megaman11_TorchMan.mp3"),
    MMX6_SIGMA("MMX6_Sigma.mp3"),
    MM6_WILY_BATTLE_MUSIC("MM6_WilyBattle.mp3"),
    MM8_FROST_MAN_MUSIC("MM8_FrostMan.mp3"),
    MM8_FROST_MAN_ALT_MUSIC("MM8_FrostMan_Alt.mp3"),
    MMX2_X_HUNTER_MUSIC("MMX2_X-Hunter.mp3"),
    MMX_SIGMA_2_STAGE("MMX_Sigma_2_Stage.mp3"),
    MMX5_VOLT_KRAKEN_MUSIC("MMX5_VoltKraken.mp3"),
    MMX_CHILL_PENGUIN_MUSIC("MMX_ChillPenguin_Famitard.mp3"),
    MMX_SIGMA_1ST_MUSIC("MMX_Sigma1st.mp3"),
    MM2_BOSS_INTRO_MUSIC("MM2_Boss_Intro.mp3"),
    MMZ_NEO_ARCADIA_MUSIC("MMZ_NeoArcadia.mp3"),
    MMZ_ENEMY_HALL_MUSIC("MMZ_EnemyHall.mp3"),
    STAGE_SELECT_MM3_MUSIC("StageSelectMM3.mp3"),
    MMX3_INTRO_STAGE_MUSIC("MMX3_IntroStage.ogg"),
    MM11_MAIN_MENU_MUSIC("MM11_Main_Menu.mp3"),
    MM_OMEGA_TITLE_THEME_MUSIC("Megaman_Omega_TitleTheme.mp3"),
    MMX_LEVEL_SELECT_SCREEN_MUSIC("MMX_LevelSelectScreen.ogg"),
    MM3_SNAKE_MAN_MUSIC("SnakeManMM3.mp3"),
    MM10_WILY1_MUSIC("MM10_Wily1.mp3"),
    MM9_GALAXY_MAN_MUSIC("MM9_GalaxyMan.mp3"),
    CHRONO_TRIGGER_WORLD_REVOLUTION("ChronoTrigger_WorldRevolution.mp3"),
    MM3_MAGNET_MAN("MM3_Magnet_Man.mp3");

    override val source = MUSIC_ASSET_PREFIX + src
    override val assClass = Music::class.java
}
