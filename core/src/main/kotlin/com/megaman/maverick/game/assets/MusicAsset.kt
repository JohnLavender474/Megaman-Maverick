package com.megaman.maverick.game.assets

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.audio.MegaAudioManager

const val MUSIC_ASSET_PREFIX = "music/"

enum class MusicAsset(src: String, val loop: Boolean = true, val onCompletion: ((MegaAudioManager) -> Unit)? = null) :
    IAsset {
    MM3_NEW_WEAPON_MUSIC("MM3_NewWeapon.wav"),
    MM6_CAPCOM_LOGO_MUSIC("MM6_CapcomLogo.mp3"),
    MM3_GAME_OVER_MUSIC("MM3_GameOver.mp3"),
    MMX6_SIGMA_2_BATTLE_INTRO_MUSIC(
        src = "MMX6_Sigma_intro.wav",
        loop = false,
        onCompletion = { it.playMusic(MMX6_SIGMA_2_BATTLE_LOOP_MUSIC, true) }
    ),
    MMX6_SIGMA_2_BATTLE_LOOP_MUSIC("MMX6_Sigma_loop.wav"),
    MM6_WILY_BATTLE_MUSIC("MM6_WilyBattle.mp3"),
    MM8_FROST_MAN_MUSIC("MM8_FrostMan.mp3"),
    MMX2_INTRO_STAGE_MUSIC("MMX2_IntroStage.mp3"),
    MM8_FROST_MAN_ALT_MUSIC("MM8_FrostMan_Alt.mp3"),
    MMX2_X_HUNTER_MUSIC("MMX2_X-Hunter.mp3"),
    MMX5_VOLT_KRAKEN_MUSIC("MMX5_VoltKraken.mp3"),
    MMX5_DARK_DIZZY_MUSIC("MMX5_DarkDizzy.mp3"),
    MMX_CHILL_PENGUIN_MUSIC("MMX_ChillPenguin_Famitard.mp3"),
    MM2_BOSS_INTRO_MUSIC("MM2_Boss_Intro.wav"),
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
    MMX2_CRYSTAL_SNAIL_MUSIC("MMX2_CrystalSnail_looping.wav"),
    INFERNO_MAN_MUSIC_OLD("vinnyz_inferno_track_OLD.mp3"),
    INFERNO_MAN_INTRO_MUSIC(
        src = "vinnyz_inferno_intro_track.wav",
        loop = false,
        onCompletion = { it.playMusic(INFERNO_MAN_LOOP_MUSIC, true) }
    ),
    INFERNO_MAN_LOOP_MUSIC("vinnyz_inferno_loop_track.wav"),
    MMX5_BOSS_FIGHT_MUSIC("MMX5_BossFight.mp3"),
    MMX7_BOSS_FIGHT_MUSIC("MMX7_BossFight.mp3"),
    MMX_SIGMA_FORTRESS_1_MUSIC("MMX_Sigma1st.mp3"),
    VINNYZ_WIP_1_MUSIC("vinnyz_wip_1.mp3"),
    MMX5_STAGE_SELECT_1_MUSIC("MMX5_StageSelect.mp3"),
    MMX5_STAGE_SELECT_2_MUSIC("MMX5_Zero_Stage_1.mp3"),
    FAMITARD_OC_1_MUSIC("Famitard_OC_1.mp3"),
    FAMITARD_OC_2_MUSIC("Famitard_OC_2.mp3"),
    CODY_O_QUINN_BATTLE_MAN_MUSIC("Cody_O_Quinn_Battle_Man.mp3"),
    CODY_O_QUINN_BATTLE_MAN_MUSIC_INTRO(
        src = "Cody_O_Quinn_Battle_Man_intro.wav",
        loop = false,
        onCompletion = { it.playMusic(CODY_O_QUINN_BATTLE_MAN_MUSIC_LOOP, true) }
    ),
    CODY_O_QUINN_BATTLE_MAN_MUSIC_LOOP("Cody_O_Quinn_Battle_Man_loop.wav"),
    CODY_O_QUINN_ACTION_MAN_MUSIC("Cody_O_Quinn_Action_Man.mp3"),
    MEGA_QUEST_2_LEVEL_1_MUSIC("Mega_Quest_2_Level_1.mp3"),
    MEGA_QUEST_2_LEVEL_2_MUSIC("Mega_Quest_2_Level_2.mp3"),
    MEGA_QUEST_2_LEVEL_5_MUSIC("Mega_Quest_2_Level_5.mp3"),
    MEGA_QUEST_2_LEVEL_6_MUSIC("Mega_Quest_2_Level_6.mp3"),
    MEGA_QUEST_2_BOSS_BATTLE_MUSIC("Mega_Quest_2_Boss_Battle.mp3"),
    CRYSTAL_MINES_MUSIC("Crystal_Mines.mp3"),
    CYBERNETIC_FACTORY_MUSIC("Cybernetic_Factory.mp3"),
    ROBOT_CITY_MUSIC("Robot_City.mp3"),
    MMZ3_GLACIER_MUSIC("MMZero3_Glacier.mp3"),
    VINNYZ_INTRO_STAGE_MUSIC("vinnyz_intro_stage.wav"),
    JX_SHADOW_DEVIL_8BIT_REMIX_MUSIC("jx_shadow_devil_8-bit_remix.mp3"),
    VINNYZ_STAGE_SELECT_V1_MUSIC("vinnyz_stage_select_v1.wav"),
    VINNYZ_WILY_STAGE_SELECT_V1_MUSIC("vinnyz_wily_stage_select_v1.wav"),
    VINNYZ_GLACIER_MUSIC("vinnyz_glacier_track.wav"),
    VINNYZ_MAIN_MENU_MUSIC("vinnyz_main_menu.wav"),
    VINNYZ_PASSWORD_MUSIC("vinnyz_password.wav"),
    VINNYZ_WEAPON_GET_MUSIC("vinnyz_weapon_get.wav");

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
