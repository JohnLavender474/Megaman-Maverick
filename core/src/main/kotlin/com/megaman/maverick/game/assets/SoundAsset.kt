package com.megaman.maverick.game.assets

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Array

const val SOUND_ASSET_PREFIX = "sounds/"

enum class SoundAsset(src: String, val seconds: Float) : IAsset {
    LIFE_SOUND("1up.mp3", 1f),
    DINK_SOUND("dink.mp3", 1f),
    ERROR_SOUND("error.mp3", 1f),
    THUMP_SOUND("thump.mp3", 1f),
    JUMP_SOUND("jump.mp3", 1f),
    CHILL_SHOOT_SOUND("chill_shoot.mp3", 1f),
    WHOOSH_SOUND("whoosh.mp3", 2f),
    PAUSE_SOUND("pause_menu.mp3", 1f),
    EXPLOSION_1_SOUND("explosion1.mp3", 2f),
    EXPLOSION_2_SOUND("explosion2.wav", 1f),
    BEAM_OUT_SOUND("teleport_out.mp3", 1f),
    ENERGY_FILL_SOUND("energy_fill_mm9.mp3", 1f),
    SELECT_PING_SOUND("select_ping.mp3", 2f),
    ENEMY_BULLET_SOUND("enemy_shoot.mp3", 1f),
    ENEMY_DAMAGE_SOUND("enemy_damage.mp3", 1f),
    MEGAMAN_LAND_SOUND("megaman_land.mp3", 1f),
    MEGAMAN_DAMAGE_SOUND("megaman_damage.mp3", 1f),
    DEFEAT_SOUND("megaman_defeat.mp3", 2f),
    SWIM_SOUND("swim.mp3", 1f),
    BOSS_DOOR_SOUND("boss_door.wav", 1f),
    CURSOR_MOVE_BLOOP_SOUND("cursor_move_bloop.mp3", 1f),
    SPLASH_SOUND("water_splash.wav", 1f),
    BEAM_SOUND("beam_in.wav", 1f),
    MEGA_BUSTER_CHARGING_SOUND("buster_charging_fadeout.wav", 4f),
    AIR_SHOOTER_SOUND("air_shooter.wav", 1f),
    ATOMIC_FIRE_SOUND("atomic_fire.wav", 1f),
    CRASH_BOMBER_SOUND("crash_bomber.wav", 1f),
    MEGA_BUSTER_BULLET_SHOT_SOUND("buster_bullet_shot.mp3", 1f),
    MEGA_BUSTER_CHARGED_SHOT_SOUND("buster_charged_shot_v2.wav", 1f),
    MM1_VICTORY_SOUND("mm1_victory.mp3", 8f),
    MM2_VICTORY_SOUND("mm2_victory.mp3", 9f),
    MM3_ELECTRIC_SAW_SOUND("electric_saw.wav", 1f),
    MM3_ELECTRIC_PULSE_SOUND("electric_pulse.mp3", 1f),
    DISAPPEARING_BLOCK_SOUND("disappearing_block.mp3", 1f),
    BURST_SOUND("burst.wav", 1f),
    TELEPORT_SOUND("teleport.wav", 1f),
    FLOATING_PORTAL_SOUND("floating_portal.wav", 1f),
    ALARM_SOUND("alarm.wav", 1f),
    TIME_STOPPER_SOUND("time_stopper.wav", 1f),
    CHOMP_SOUND("chomp.wav", 1f),
    BLOOPITY_SOUND("bloopity.wav", 1f),
    MM2_MECHA_DRAGON_SOUND("mecha_dragon.mp3", 1f),
    BLAST_1_SOUND("blast1.wav", 1f),
    BLAST_2_SOUND("blast2.wav", 1f),
    BASSY_BLAST_SOUND("bassy_blast.wav", 1f),
    LIFT_OFF_SOUND("liftoff.wav", 2f),
    CONVEYOR_LIFT_SOUND("conveyor_lift.mp3", 1f),
    QUAKE_SOUND("quake.mp3", 1f),
    ICE_SHARD_1_SOUND("ice_shard_1.wav", 1f),
    ICE_SHARD_2_SOUND("ice_shard_2.wav", 1f),
    JETPACK_SOUND("jetpack.mp3", 1f),
    MARIO_FIREBALL_SOUND("mario_fireball.mp3", 1f),
    FLAMETHROWER_SOUND("flamethrower.wav", 2f),
    SHAKE_SOUND("shake.wav", 1f),
    WHEE_SOUND("whee.wav", 1f),
    WIND_1_SOUND("wind_1.wav", 3f),
    WHIP_SOUND("whip.wav", 1f),
    WHIP_V2_SOUND("whip_v2.mp3", 1f),
    ASTEROID_EXPLODE_SOUND("asteroid_explode.wav", 1f),
    SOLAR_BLAZE_SOUND("solar_blaze.wav", 1f),
    BRUSH_SOUND("brush.wav", 1f),
    BUTTON_SOUND("button.wav", 1f),
    SPACE_LAZER_SOUND("space_lazer.wav", 1f),
    CURRENCY_PICKUP_SOUND("currency_pickup.wav", 1f),
    ONE_UP_SOUND("one_up.wav", 1f),
    REV_SOUND("rev.mp3", 1f),
    VOLT_SOUND("volt.wav", 1f),
    ELECTRIC_1_SOUND("electric_1.wav", 1f),
    ELECTRIC_2_SOUND("electric_2.wav", 2f),
    POUND_SOUND("pound.wav", 1f),
    BLACKHOLE_SOUND("blackhole.wav", 0.35f);

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
