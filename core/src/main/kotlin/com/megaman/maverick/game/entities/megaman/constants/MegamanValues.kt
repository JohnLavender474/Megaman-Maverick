package com.megaman.maverick.game.entities.megaman.constants

import com.badlogic.gdx.graphics.Color
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.megaman.maverick.game.controllers.MegaControllerButton

object MegamanValues {

    const val START_HEALTH = 14
    const val MAX_WEAPON_AMMO = 30

    const val SLIP_SLIDE_THRESHOLD = 0.25f

    const val CLAMP_X = 16f
    const val CLAMP_Y = 40f

    const val RUN_1_SCALAR = 0.75f
    const val RUN_1_TIME = 0.1f
    const val RUN_2_SCALAR = 0.9f
    const val RUN_2_TIME = 0.25f
    const val RUN_3_SCALAR = 1f

    const val RUN_MAX_SPEED = 6f
    const val RUN_IMPULSE = 40f
    const val ICE_RUN_IMPULSE = 15f
    const val WATER_RUN_MAX_SPEED_SCALAR = 0.5f
    const val MOON_RUN_MAX_SPEED_SCALAR = 0.75f

    const val WATER_GRAVITY_SCALAR = 0.5f
    const val SWIM_VEL_Y = 18f
    const val SWIM_TIMER = 0.25f

    const val JUMP_VEL = 25f
    const val WALL_JUMP_VEL = 35f
    const val WALL_JUMP_HORIZONTAL = 6f
    const val WALL_JUMP_IMPETUS_TIME = 0.1f
    const val WALL_SLIDE_FRICTION_TO_APPLY = 2.5f
    const val WALL_SLIDE_GRAVITY = -0.125f

    const val GROUND_GRAVITY = -0.001f
    const val JUMP_GRAVITY = -0.2f
    const val FALL_GRAVITY = -0.5f
    const val ICE_GRAVITY = -0.75f
    const val WATER_GRAVITY = -0.25f
    const val WATER_ICE_GRAVITY = -0.5f

    const val AIR_DASH_MIN_TIME = 0.1f
    const val AIR_DASH_MAX_TIME = 0.25f
    const val AIR_DASH_VEL = 10f
    const val AIR_DASH_END_BUMP = 2f
    const val WATER_AIR_DASH_VEL = 5f
    const val WATER_AIR_DASH_END_BUMP = 1f

    const val JETPACK_Y_IMPULSE = 15f
    const val JETPACK_TIME_PER_BIT = 0.5f

    const val SHIELD_GEM_DISTANCE_DELTA = 5f
    const val SHIELD_GEM_MAX_DIST = 10f
    const val SHIELD_GEM_LERP = 10f

    const val AXE_IMPULSE_X = 8f
    const val AXE_IMPULSE_Y = 20f
    const val AXE_SLOW_SCALAR = 0.5f

    const val GROUND_SLIDE_COOLDOWN = 0.075f
    const val GROUND_SLIDE_MIN_TIME = 0.2f
    const val GROUND_SLIDE_MAX_TIME = 0.5f
    const val GROUND_SLIDE_VEL = 10f
    const val WATER_GROUND_SLIDE_VEL = 6f

    const val CLIMB_VEL = 5f

    const val DAMAGE_DURATION = 0.5f
    const val DAMAGE_RECOVERY_TIME = 1.5f
    const val DAMAGE_FLASH_DURATION = 0.05f

    const val TIME_TO_HALFWAY_CHARGED = 0.75f
    const val TIME_TO_FULLY_CHARGED = 1.5f

    const val SHOOT_ANIM_TIME = 0.3f

    const val DMG_X = 8f
    const val DMG_Y = 5f

    const val EXPLOSION_ORB_SPEED = 3.5f

    const val BULLET_VEL = 12f
    const val ICE_CUBE_VEL = 12f
    const val MAGMA_WAVE_VEL = 10f
    const val MOON_SCYTHE_SPEED = 8f
    const val MAX_MOONS_BEFORE_SHOOT_AGAIN = 1
    val MOON_SCYTHE_DEG_OFFSETS = gdxArrayOf(10f, 30f, 50f, 70f)

    const val SPAWNING_DUR = 0.05f

    const val CROUCH_DELAY = 0.05f
    const val CROUCH_MAX_VEL = 1f

    const val STUN_DUR = 0.25f
    const val STUN_IMPULSE_X = 2.5f
    const val STUN_IMPULSE_Y = 2f

    const val WALLSLIDE_NOT_ALLOWED_DELAY_ON_BOUNCE = 0.1f

    const val HITS_TO_UNFREEZE = 3
    const val FROZEN_PUSH_DUR = 0.2f
    val BUTTONS_TO_UNFREEZE = gdxArrayOf<Any>(MegaControllerButton.B)

    val NEEDLE_ANGLES = gdxArrayOf(90f, 70f, 45f, 15f, 0f, 345f, 315f, 290f, 270f)
    val NEEDLE_X_OFFSETS = gdxArrayOf(-0.2f, -0.15f, -0.1f, -0.05f, 0f, 0.05f, 0.1f, 0.15f, 0.2f)
    const val NEEDLE_GRAV = -0.1f
    const val NEEDLE_IMPULSE = 15f
    const val NEEDLE_Y_OFFSET = 1f
    const val NEEDLE_SPIN_MEGAMAN_IMPULSE_Y = 20f
    const val NEEDLE_SPIN_WATER_SCALAR = 0.1f

    const val SHOOT_DOWN_IMPULSE_Y_CHARGED = 25f

    val WEAPON_SPAWN_MAGIC_COLOR: Color = Color.valueOf("dd00ffff")
}
