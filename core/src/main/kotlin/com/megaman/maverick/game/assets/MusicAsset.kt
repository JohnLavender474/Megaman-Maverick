package com.megaman.maverick.game.assets

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.audio.MegaAudioManager

const val MUSIC_ASSET_PREFIX = "music/"

enum class MusicAsset(src: String, val loop: Boolean = true, val onCompletion: ((MegaAudioManager) -> Unit)? = null) :
    IAsset {
    MM3_NEW_WEAPON_MUSIC("MM3_NewWeapon.ogg"),
    MM6_CAPCOM_LOGO_MUSIC("MM6_CapcomLogo.ogg"),
    MM3_GAME_OVER_MUSIC("MM3_GameOver.ogg"),
    MMX6_SIGMA_2_BATTLE_INTRO_MUSIC(
        src = "MMX6_Sigma_intro.ogg",
        loop = false,
        onCompletion = { it.playMusic(MMX6_SIGMA_2_BATTLE_LOOP_MUSIC, true) }
    ),
    MMX6_SIGMA_2_BATTLE_LOOP_MUSIC("MMX6_Sigma_loop.ogg"),
    MM6_WILY_BATTLE_MUSIC("MM6_WilyBattle.ogg"),
    MM8_FROST_MAN_MUSIC("MM8_FrostMan.ogg"),
    MMX2_INTRO_STAGE_MUSIC("MMX2_IntroStage.ogg"),
    MM8_FROST_MAN_ALT_MUSIC("MM8_FrostMan_Alt.ogg"),
    MMX2_X_HUNTER_MUSIC("MMX2_X-Hunter.ogg"),
    MMX5_VOLT_KRAKEN_MUSIC("MMX5_VoltKraken.ogg"),
    MMX5_DARK_DIZZY_MUSIC("MMX5_DarkDizzy.ogg"),
    MMX_CHILL_PENGUIN_MUSIC("MMX_ChillPenguin_Famitard.ogg"),
    MM2_BOSS_INTRO_MUSIC("MM2_Boss_Intro.ogg"),
    MMZ_ENEMY_HALL_MUSIC("MMZ_EnemyHall.ogg"),
    STAGE_SELECT_MM3_MUSIC("StageSelectMM3.ogg"),
    MMX3_INTRO_STAGE_MUSIC("MMX3_IntroStage.ogg"),
    MM3_SNAKE_MAN_MUSIC("SnakeManMM3.ogg"),
    MM10_WILY1_MUSIC("MM10_Wily1.ogg"),
    MM9_GALAXY_MAN_MUSIC("MM9_GalaxyMan.ogg"),
    MM3_MAGNET_MAN_MUSIC("MM3_Magnet_Man.ogg"),
    MM2_PASSWORD_SCREEN_MUSIC("MM2_PasswordScreen.ogg"),
    MMX6_BLAZE_HEATNIX_MUSIC("MMX6_BlazeHeatnix.ogg"),
    MM2_CREDITS_MUSIC("MM2_Credits.ogg"),
    MMX7_VANISHING_GUNGAROO_MUSIC("MMX7_VanishingGungaroo.ogg"),
    MMX8_BURN_ROOSTER_MUSIC("MMX8_BurnRooster.ogg"),
    MMX5_IZZY_GLOW_MUSIC("MMX5_IzzyGlow.ogg"),
    MMX6_GATE_STAGE_MUSIC("MMX6_Gate_Stage.ogg"),
    MMX6_BOSS_FIGHT_MUSIC("MMX6_BossFight.ogg"),
    MMX2_MORPH_MOTH_MUSIC("MMX2_MorphMoth.ogg"),
    MM7_FINAL_BOSS_INTRO_MUSIC(
        src = "MM7_Final_Boss_Intro.ogg",
        loop = false,
        onCompletion = { it.playMusic(MM7_FINAL_BOSS_LOOP_MUSIC, true) }
    ),
    MM7_FINAL_BOSS_LOOP_MUSIC("MM7_Final_Boss_Loop.ogg"),
    MMX_VILE_MUSIC("MMX_Vile.ogg"),
    MM7_SLASH_MAN_MUSIC("MM7_SlashMan.ogg"),
    MMX3_NEON_TIGER_MUSIC("MMX3_NeonTiger.ogg"),
    MMX2_CRYSTAL_SNAIL_MUSIC("MMX2_CrystalSnail_looping.ogg"),
    INFERNO_MAN_MUSIC_OLD("vinnyz_inferno_track_OLD.ogg"),
    INFERNO_MAN_INTRO_MUSIC(
        src = "vinnyz_inferno_intro_track.ogg",
        loop = false,
        onCompletion = { it.playMusic(INFERNO_MAN_LOOP_MUSIC, true) }
    ),
    INFERNO_MAN_LOOP_MUSIC("vinnyz_inferno_loop_track.ogg"),
    MMX5_BOSS_FIGHT_MUSIC("MMX5_BossFight.ogg"),
    MMX7_BOSS_FIGHT_MUSIC("MMX7_BossFight.ogg"),
    MMX_SIGMA_FORTRESS_1_MUSIC("MMX_Sigma1st.ogg"),
    VINNYZ_WIP_1_MUSIC("vinnyz_wip_1.ogg"),
    MMX5_STAGE_SELECT_1_MUSIC("MMX5_StageSelect.ogg"),
    MMX5_STAGE_SELECT_2_MUSIC("MMX5_Zero_Stage_1.ogg"),
    FAMITARD_OC_1_MUSIC("Famitard_OC_1.ogg"),
    FAMITARD_OC_2_MUSIC("Famitard_OC_2.ogg"),
    CODY_O_QUINN_BATTLE_MAN_MUSIC("Cody_O_Quinn_Battle_Man.ogg"),
    CODY_O_QUINN_BATTLE_MAN_MUSIC_INTRO(
        src = "Cody_O_Quinn_Battle_Man_intro.ogg",
        loop = false,
        onCompletion = { it.playMusic(CODY_O_QUINN_BATTLE_MAN_MUSIC_LOOP, true) }
    ),
    CODY_O_QUINN_BATTLE_MAN_MUSIC_LOOP("Cody_O_Quinn_Battle_Man_loop.ogg"),
    CODY_O_QUINN_ACTION_MAN_MUSIC("Cody_O_Quinn_Action_Man.ogg"),
    MEGA_QUEST_2_LEVEL_1_MUSIC("Mega_Quest_2_Level_1.ogg"),
    MEGA_QUEST_2_LEVEL_2_MUSIC("Mega_Quest_2_Level_2.ogg"),
    MEGA_QUEST_2_LEVEL_5_MUSIC("Mega_Quest_2_Level_5.ogg"),
    MEGA_QUEST_2_LEVEL_6_MUSIC("Mega_Quest_2_Level_6.ogg"),
    MEGA_QUEST_2_BOSS_BATTLE_MUSIC("Mega_Quest_2_Boss_Battle.ogg"),
    CRYSTAL_MINES_MUSIC("Crystal_Mines.ogg"),
    CYBERNETIC_FACTORY_MUSIC("Cybernetic_Factory.ogg"),
    ROBOT_CITY_MUSIC("Robot_City.ogg"),
    MMZ3_GLACIER_MUSIC("MMZero3_Glacier.ogg"),
    VINNYZ_INTRO_STAGE_MUSIC("vinnyz_intro_stage.ogg"),
    JX_SHADOW_DEVIL_8BIT_REMIX_MUSIC("jx_shadow_devil_8-bit_remix.ogg"),
    VINNYZ_STAGE_SELECT_V1_MUSIC("vinnyz_stage_select_v1.ogg"),
    VINNYZ_WILY_STAGE_SELECT_V1_MUSIC("vinnyz_wily_stage_select_v1.ogg"),
    VINNYZ_GLACIER_MUSIC("vinnyz_glacier_track.ogg"),
    VINNYZ_MAIN_MENU_MUSIC("vinnyz_main_menu_theme_v2.ogg"),
    VINNYZ_PASSWORD_MUSIC("vinnyz_password.ogg"),
    VINNYZ_WEAPON_GET_MUSIC("vinnyz_weapon_get.ogg"),
    SMB3_OVERWORLD_1_MUSIC("smb3_overworld_1.ogg"),
    SMB3_UNDERGROUND_MUSIC("smb3_underground.ogg"),
    MMX7_OUR_BLOOD_BOILS_MUSIC("MMX7_our_blood_boils.ogg"),
    OUR_BLOOD_BOILS_8_BIT("our_blood_boils_8bit_metallicwarrior.ogg"),
    MM3_ALL_CLEAR_MUSIC("mm3_all_clear.ogg", loop = false),
    MM2_END_THEME_MUSIC("mm2_end_theme.ogg"),
    MMX3_ZERO_THEME_MUSIC("mmx3_zero_theme.ogg");

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
