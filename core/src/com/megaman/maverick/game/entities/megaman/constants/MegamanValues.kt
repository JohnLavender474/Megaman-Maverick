package com.megaman.maverick.game.entities.megaman.constants

/** Constant values for Megaman. */
object MegamanValues {
  const val MAX_HEALTH_TANKS = 4
  const val MAX_WEAPON_AMMO = 30
  const val START_HEALTH = 14
  const val MAX_HELTH = 30

  const val CLAMP_X = 15f
  const val CLAMP_Y = 25f

  const val RUN_SPEED = 5f
  const val RUN_IMPULSE = 50f
  const val ICE_RUN_IMPULSE = 15f
  const val WATER_RUN_SPEED = 2.25f

  const val SWIM_VEL_Y = 20f

  const val JUMP_VEL = 24f
  const val WATER_JUMP_VEL = 28f
  const val WATER_WALL_JUMP_VEL = 38f
  const val WALL_JUMP_VEL = 42f

  const val WALL_JUMP_HORIZONTAL = 10f
  const val WALL_JUMP_IMPETUS_TIME = .1f

  const val GROUND_GRAVITY = -.0015f
  const val GRAVITY = -.375f
  const val ICE_GRAVITY = -.5f
  const val WATER_GRAVITY = -.25f
  const val WATER_ICE_GRAVITY = -.4f

  const val AIR_DASH_VEL = 12f
  const val AIR_DASH_END_BUMP = 3f
  const val WATER_AIR_DASH_VEL = 6f
  const val WATER_AIR_DASH_END_BUMP = 2f
  const val MAX_AIR_DASH_TIME = .25f

  const val GROUND_SLIDE_VEL = 12f
  const val WATER_GROUND_SLIDE_VEL = 6f
  const val MAX_GROUND_SLIDE_TIME = .35f

  const val CLIMB_VEL = 2.5f

  const val DAMAGE_DURATION = .75f
  const val DAMAGE_RECOVERY_TIME = 1.5f
  const val DAMAGE_FLASH_DURATION = .05f

  const val TIME_TO_HALFWAY_CHARGED = .5f
  const val TIME_TO_FULLY_CHARGED = 1.25f

  const val SHOOT_ANIM_TIME = .3f

  const val DMG_X = 8f
  const val DMG_Y = 5f
}
