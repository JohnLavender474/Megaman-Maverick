package com.megaman.maverick.game.assets

import com.badlogic.gdx.audio.Sound

const val SOUND_ASSET_PREFIX = "sounds/"

/** An enum class representing sound assets. */
enum class SoundAsset(src: String, val seconds: Int) : IAsset {
  LIFE_SOUND("1up.mp3", 1),
  DINK_SOUND("Dink.mp3", 1),
  ERROR_SOUND("Error.mp3", 1),
  THUMP_SOUND("Thump.mp3", 1),
  WALL_JUMP("WallJump.mp3", 1),
  WHOOSH_SOUND("Whoosh.mp3", 2),
  PAUSE_SOUND("PauseMenu.mp3", 1),
  CHILL_SHOOT("ChillShoot.mp3", 1),
  EXPLOSION_SOUND("Explosion.mp3", 2),
  BEAM_OUT_SOUND("TeleportOut.mp3", 1),
  ENERGY_FILL_SOUND("EnergyFill.mp3", 2),
  SELECT_PING_SOUND("SelectPing.mp3", 2),
  ENEMY_BULLET_SOUND("EnemyShoot.mp3", 1),
  ENEMY_DAMAGE_SOUND("EnemyDamage.mp3", 1),
  MEGAMAN_LAND_SOUND("MegamanLand.mp3", 1),
  ACID_SOUND("Megaman_2_Sounds/acid.wav", 1),
  MEGAMAN_DAMAGE_SOUND("MegamanDamage.mp3", 1),
  MEGAMAN_DEFEAT_SOUND("MegamanDefeat.mp3", 2),
  SWIM_SOUND("SuperMarioBros/smb_stomp.mp3", 1),
  BOSS_DOOR("Megaman_2_Sounds/boss_door.wav", 1),
  CURSOR_MOVE_BLOOP_SOUND("CursorMoveBloop.mp3", 1),
  SPLASH_SOUND("Megaman_2_Sounds/water_splash.mp3", 1),
  BEAM_IN_SOUND("Megaman_2_Sounds/teleport_in.wav", 1),
  MEGA_BUSTER_CHARGING_SOUND("MegaBusterCharging.mp3", 12),
  AIR_SHOOTER_SOUND("Megaman_2_Sounds/air_shooter.wav", 1),
  ATOMIC_FIRE_SOUND("Megaman_2_Sounds/atomic_fire.wav", 1),
  CRASH_BOMBER_SOUND("Megaman_2_Sounds/crash_bomber.wav", 1),
  MEGA_BUSTER_BULLET_SHOT_SOUND("MegaBusterBulletShot.mp3", 1),
  MEGA_BUSTER_CHARGED_SHOT_SOUND("MegaBusterChargedShot.mp3", 1);

  override val source = SOUND_ASSET_PREFIX + src
  override val assClass = Sound::class.java
}
