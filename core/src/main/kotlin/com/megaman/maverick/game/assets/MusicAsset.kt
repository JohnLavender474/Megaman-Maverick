package com.megaman.maverick.game.com.megaman.maverick.game.assets

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.utils.Array

const val MUSIC_ASSET_PREFIX = "music/"

enum class MusicAsset(src: String) : IAsset {
    MMX6_SIGMA_2_BATTLE_MUSIC("MMX6_Sigma.mp3"),
    MM6_WILY_BATTLE_MUSIC("MM6_WilyBattle.mp3"),
    MM8_FROST_MAN_MUSIC("MM8_FrostMan.mp3"),
    MM8_FROST_MAN_ALT_MUSIC("MM8_FrostMan_Alt.mp3"),
    MMX2_X_HUNTER_MUSIC("MMX2_X-Hunter.mp3"),
    MMX5_VOLT_KRAKEN_MUSIC("MMX5_VoltKraken.mp3"),
    MMX5_DARK_DIZZY_MUSIC("MMX5_DarkDizzy.mp3"),
    MMX_CHILL_PENGUIN_MUSIC("MMX_ChillPenguin_Famitard.mp3"),
    MM2_BOSS_INTRO_MUSIC("MM2_Boss_Intro.mp3"),
    MMZ_ENEMY_HALL_MUSIC("MMZ_EnemyHall.mp3"),
    STAGE_SELECT_MM3_MUSIC("StageSelectMM3.mp3"),
    MMX3_INTRO_STAGE_MUSIC("MMX3_IntroStage.ogg"),
    MM3_SNAKE_MAN_MUSIC("SnakeManMM3.mp3"),
    MM10_WILY1_MUSIC("MM10_Wily1.mp3"),
    MM9_GALAXY_MAN_MUSIC("MM9_GalaxyMan.mp3"),
    MM3_MAGNET_MAN_MUSIC("MM3_Magnet_Man.mp3"),
    MM2_PASSWORD_SCREEN_MUSIC("MM2_PasswordScreen.mp3"),
    MMX6_BLAZE_HEATNIX_MUSIC("MMX6_BlazeHeatnix.mp3"),
    MM2_CREDITS_MUSIC("MM2_Credits.mp3"),
    MMX7_VANISHING_GUNGAROO_MUSIC("MMX7_VanishingGungaroo.mp3"),
    MMX8_BURN_ROOSTER_MUSIC("MMX8_BurnRooster.mp3"),
    MMX5_IZZY_GLOW_MUSIC("MMX5_IzzyGlow.mp3"),
    MMX6_GATE_STAGE_MUSIC("MMX6_Gate_Stage.mp3"),
    MMX6_BOSS_FIGHT_MUSIC("MMX6_BossFight.mp3"),
    MMX2_MORPH_MOTH_MUSIC("MMX2_MorphMoth.mp3"),
    MM7_FINAL_BOSS_INTRO_MUSIC("MM7_Final_Boss_Intro.mp3"),
    MM7_FINAL_BOSS_LOOP_MUSIC("MM7_Final_Boss_Loop.mp3"),
    MMX_VILE_MUSIC("MMX_Vile.mp3"),
    MM7_SLASH_MAN_MUSIC("MM7_SlashMan.mp3"),
    MMX3_NEON_TIGER_MUSIC("MMX3_NeonTiger.mp3"),
    MMX2_CRYSTAL_SNAIL_MUSIC("MMX2_CrystalSnail.mp3"),
    INFERNO_MAN_MUSIC("vinnyz_inferno_track.mp3");

    companion object {
        fun valuesAsIAssetArray(): Array<IAsset> {
            val assets = Array<IAsset>()
            MusicAsset.entries.forEach { assets.add(it) }
            return assets
        }
    }

    override val source = MUSIC_ASSET_PREFIX + src
    override val assClass = Music::class.java
}
