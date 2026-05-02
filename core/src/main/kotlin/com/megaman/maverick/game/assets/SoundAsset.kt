package com.megaman.maverick.game.assets

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Array

const val SOUND_ASSET_PREFIX = "sounds/"

enum class SoundAsset(
    src: String,
    val seconds: Float,
    val defaultAllowOverlap: Boolean = false
) : IAsset {
    LIFE_SOUND("1up.ogg", 1f),
    DINK_SOUND("dink.ogg", 0.05f),
    ERROR_SOUND("error.ogg", 0.3f),
    THUMP_SOUND("thump.ogg", 0.1f),
    JUMP_SOUND("jump.ogg", 0.1f),
    CHILL_SHOOT_SOUND("chill_shoot.ogg", 0.2f),
    WHOOSH_SOUND("whoosh.ogg", 0.8f),
    PAUSE_SOUND("pause_menu.ogg", 0.25f),
    EXPLOSION_1_SOUND("explosion1.ogg", 1.25f),
    EXPLOSION_2_SOUND("explosion2.ogg", 0.5f),
    BEAM_OUT_SOUND("teleport_out.ogg", 1.35f),
    ENERGY_FILL_SOUND("energy_fill_mm9.ogg", 0.05f),
    SELECT_PING_SOUND("select_ping.ogg", 0.65f),
    ENEMY_BULLET_SOUND("enemy_shoot.ogg", 0.25f),
    ENEMY_DAMAGE_SOUND("enemy_damage.ogg", 0.5f),
    CHARGED_SHOT_EXPLODE_SOUND("enemy_damage.ogg", 0.5f, true),
    MEGAMAN_LAND_SOUND("megaman_land.ogg", 0.2f),
    MEGAMAN_DAMAGE_SOUND("megaman_damage.ogg", 0.4f),
    DEFEAT_SOUND("megaman_defeat.ogg", 2f),
    SWIM_SOUND("swim.ogg", 0.25f),
    BOSS_DOOR_SOUND("boss_door.ogg", 1f),
    CURSOR_MOVE_BLOOP_SOUND("cursor_move_bloop.ogg", 0.25f),
    SPLASH_SOUND("water_splash.ogg", 1f),
    BEAM_SOUND("beam_in.ogg", 1f),
    MEGA_BUSTER_CHARGING_SOUND("buster_charging_fadeout.ogg", 4.5f),
    AIR_SHOOTER_SOUND("air_shooter.ogg", 1f),
    ATOMIC_FIRE_SOUND("atomic_fire.ogg", 1f),
    CRASH_BOMBER_SOUND("crash_bomber.ogg", 1f),
    MEGA_BUSTER_BULLET_SHOT_SOUND("buster_bullet_shot.ogg", 0.2f),
    MEGA_BUSTER_CHARGED_SHOT_SOUND("buster_charged_shot_v2.ogg", 1.2f),
    MM1_VICTORY_SOUND("mm1_victory.ogg", 8f),
    MM2_VICTORY_SOUND("mm2_victory.ogg", 9f),
    MM3_ELECTRIC_SAW_SOUND("electric_saw.ogg", 1f),
    MM3_ELECTRIC_PULSE_SOUND("electric_pulse.ogg", 1f),
    DISAPPEARING_BLOCK_SOUND("disappearing_block.ogg", 1f),
    BURST_SOUND("burst.ogg", 1f),
    TELEPORT_SOUND("teleport.ogg", 1f),
    FLOATING_PORTAL_SOUND("floating_portal.ogg", 1f),
    ALARM_SOUND("alarm.ogg", 1f),
    TIME_STOPPER_SOUND("time_stopper.ogg", 1f),
    CHOMP_SOUND("chomp.ogg", 1f),
    BLOOPITY_SOUND("bloopity.ogg", 1f),
    MM2_MECHA_DRAGON_SOUND("mecha_dragon.mp3", 1f),
    BLAST_1_SOUND("blast1.ogg", 1f),
    BLAST_2_SOUND("blast2.ogg", 1f),
    BASSY_BLAST_SOUND("bassy_blast.ogg", 1f),
    LIFT_OFF_SOUND("liftoff.ogg", 2f),
    CONVEYOR_LIFT_SOUND("conveyor_lift.ogg", 1f),
    QUAKE_SOUND("quake.mp3", 1f),
    ICE_SHARD_1_SOUND("ice_shard_1.ogg", 1f),
    ICE_SHARD_2_SOUND("ice_shard_2.ogg", 1f),
    JETPACK_SOUND("jetpack.ogg", 0.35f),
    MARIO_FIREBALL_SOUND("mario_fireball.ogg", 1f),
    FLAMETHROWER_SOUND("flamethrower.ogg", 2f),
    SHAKE_SOUND("shake.ogg", 1f),
    WHEE_SOUND("whee.ogg", 1f),
    WIND_1_SOUND("wind_1.ogg", 3f),
    WHIP_SOUND("whip.ogg", 1f),
    WHIP_V2_SOUND("whip_v2.ogg", 1f),
    ASTEROID_EXPLODE_SOUND("asteroid_explode.ogg", 0.5f),
    SOLAR_BLAZE_SOUND("solar_blaze.ogg", 1f),
    BRUSH_SOUND("brush.ogg", 1f),
    BUTTON_SOUND("button.ogg", 1f),
    SPACE_LAZER_SOUND("space_lazer.ogg", 1f),
    CURRENCY_PICKUP_SOUND("currency_pickup.ogg", 1f),
    ONE_UP_SOUND("one_up.ogg", 1f),
    REV_SOUND("rev.ogg", 1f),
    VOLT_SOUND("volt.ogg", 1f),
    ELECTRIC_1_SOUND("electric_1.ogg", 1f),
    ELECTRIC_2_SOUND("electric_2.ogg", 2f),
    POUND_SOUND("pound.ogg", 1f),
    BLACKHOLE_SOUND("blackhole.ogg", 0.35f),
    DIG_SOUND("dig.ogg", 0.25f),
    JET_SOUND("jet.ogg", 1f),
    LASER_BEAM_SOUND("laser_beam.ogg", 1f),
    SMB3_BUMP_SOUND("smb3_bump.ogg", 0.2f),
    SMB3_KICK_SOUND("smb3_kick.ogg", 0.2f),
    SMB3_JUMP_SOUND("smb3_jump.ogg", 0.7f),
    SMB3_PLAYER_DOWN_SOUND("smb3_player_down.ogg", 3.25f),
    SMB3_THWOMP_SOUND("smb3_thwomp.ogg", 0.4f),
    SMB3_PIPE_SOUND("smb3_pipe.ogg", 0.75f),
    SMB3_COIN_SOUND("smb3_coin.ogg", 0.9f),
    SMB3_BEANSTALK_SOUND("smb3_beanstalk.ogg", 1.10f);

    companion object {
        fun valuesAsIAssetArray(): Array<IAsset> {
            val assets = Array<IAsset>()
            SoundAsset.entries.forEach { assets.add(it) }
            return assets
        }
    }

    override val source = SOUND_ASSET_PREFIX + src
    override val assClass = Sound::class.java
}
