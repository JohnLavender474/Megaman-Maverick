package com.megaman.maverick.game.assets

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Array

const val SOUND_ASSET_PREFIX = "sounds/"

enum class SoundAsset(src: String, val seconds: Int) : IAsset {
    LIFE_SOUND("1up.mp3", 1),
    DINK_SOUND("dink.mp3", 1),
    ERROR_SOUND("error.mp3", 1),
    THUMP_SOUND("thump.mp3", 1),
    WALL_JUMP_SOUND("wall_jump.mp3", 1),
    CHILL_SHOOT_SOUND("chill_shoot.mp3", 1),
    WHOOSH_SOUND("whoosh.mp3", 2),
    PAUSE_SOUND("pause_menu.mp3", 1),
    EXPLOSION_1_SOUND("explosion1.mp3", 2),
    EXPLOSION_2_SOUND("explosion2.wav", 1),
    BEAM_OUT_SOUND("teleport_out.mp3", 1),
    ENERGY_FILL_SOUND("energy_fill.mp3", 2),
    SELECT_PING_SOUND("select_ping.mp3", 2),
    ENEMY_BULLET_SOUND("enemy_shoot.mp3", 1),
    ENEMY_DAMAGE_SOUND("enemy_damage.mp3", 1),
    MEGAMAN_LAND_SOUND("megaman_land.mp3", 1),
    MEGAMAN_DAMAGE_SOUND("megaman_damage.mp3", 1),
    DEFEAT_SOUND("megaman_defeat.mp3", 2),
    SWIM_SOUND("swim.mp3", 1),
    BOSS_DOOR_SOUND("boss_door.wav", 1),
    CURSOR_MOVE_BLOOP_SOUND("cursor_move_bloop.mp3", 1),
    SPLASH_SOUND("water_splash.wav", 1),
    BEAM_SOUND("beam_in.wav", 1),
    MEGA_BUSTER_CHARGING_SOUND("buster_charging_fadeout.wav", 4),
    AIR_SHOOTER_SOUND("air_shooter.wav", 1),
    ATOMIC_FIRE_SOUND("atomic_fire.wav", 1),
    CRASH_BOMBER_SOUND("crash_bomber.wav", 1),
    MEGA_BUSTER_BULLET_SHOT_SOUND("buster_bullet_shot.mp3", 1),
    MEGA_BUSTER_CHARGED_SHOT_SOUND("buster_charged_shot.mp3", 1),
    MM1_VICTORY_SOUND("mm1_victory.mp3", 8),
    MM3_ELECTRIC_SAW_SOUND("electric_saw.wav", 1),
    MM3_ELECTRIC_PULSE_SOUND("electric_pulse.mp3", 1),
    DISAPPEARING_BLOCK_SOUND("disappearing_block.mp3", 1),
    BURST_SOUND("burst.wav", 1),
    TELEPORT_SOUND("teleport.wav", 1),
    ALARM_SOUND("alarm.wav", 1),
    TIME_STOPPER_SOUND("time_stopper.wav", 1),
    CHOMP_SOUND("chomp.wav", 1),
    BLOOPITY_SOUND("bloopity.wav", 1),
    MM2_MECHA_DRAGON_SOUND("mecha_dragon.mp3", 1),
    BLAST_SOUND("blast.wav", 1),
    BASSY_BLAST_SOUND("bassy_blast.wav", 1),
    LIFT_OFF_SOUND("liftoff.wav", 2),
    CONVEYOR_LIFT_SOUND("conveyor_lift.mp3", 1),
    QUAKE_SOUND("quake.mp3", 1),
    ICE_SHARD_1_SOUND("ice_shard_1.wav", 1),
    ICE_SHARD_2_SOUND("ice_shard_2.wav", 1),
    JETPACK_SOUND("jetpack.mp3", 1),
    MARIO_FIREBALL_SOUND("mario_fireball.mp3", 1),
    FLAMETHROWER_SOUND("flamethrower.wav", 2),
    SHAKE_SOUND("shake.wav", 1),
    WHEE_SOUND("whee.wav", 1);

    companion object {
        fun valuesAsIAssetArray(): Array<IAsset> {
            val assets = Array<IAsset>()
            values().forEach { assets.add(it) }
            return assets
        }
    }

    override val source = SOUND_ASSET_PREFIX + src
    override val assClass = Sound::class.java
}