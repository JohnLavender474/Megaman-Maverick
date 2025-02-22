package com.megaman.maverick.game.entities.megaman.constants

import com.mega.game.engine.common.extensions.gdxArrayOf
import com.megaman.maverick.game.controllers.MegaControllerButton

object MegamanValues {

    const val START_HEALTH = 14
    const val MAX_WEAPON_AMMO = 30

    const val SLIP_SLIDE_VEL_THRESHOLD = 0.25f

    const val CLAMP_X = 15f
    const val CLAMP_Y = 40f

    const val RUN_SPEED = 5f
    const val RUN_IMPULSE = 35f
    const val ICE_RUN_IMPULSE = 15f
    const val WATER_RUN_SPEED = 2.25f

    const val WATER_GRAVITY_SCALAR = 0.5f
    const val SWIM_VEL_Y = 14f

    const val JUMP_VEL = 24f
    const val WALL_JUMP_VEL = 34f

    const val WALL_SLIDE_FRICTION_TO_APPLY = 2.5f

    const val WALL_JUMP_HORIZONTAL = 6f
    const val WALL_JUMP_IMPETUS_TIME = 0.1f

    const val GROUND_GRAVITY = -0.001f
    const val WALL_SLIDE_GRAVITY = -0.1f
    const val JUMP_GRAVITY = -0.2f
    const val FALL_GRAVITY = -0.5f
    const val ICE_GRAVITY = -0.75f
    const val WATER_GRAVITY = -0.25f
    const val WATER_ICE_GRAVITY = -0.5f

    const val MAX_AIR_DASH_TIME = 0.25f
    const val AIR_DASH_VEL = 8f
    const val AIR_DASH_END_BUMP = 2f
    const val WATER_AIR_DASH_VEL = 5f
    const val WATER_AIR_DASH_END_BUMP = 1f

    const val JETPACK_Y_IMPULSE = 15f
    const val JETPACK_TIME_PER_BIT = 0.25f

    const val GROUND_SLIDE_VEL = 10f
    const val WATER_GROUND_SLIDE_VEL = 5f
    const val MAX_GROUND_SLIDE_TIME = 0.35f

    const val CLIMB_VEL = 3.25f

    const val DAMAGE_DURATION = .75f
    const val DAMAGE_RECOVERY_TIME = 1.5f
    const val DAMAGE_FLASH_DURATION = 0.05f

    const val TIME_TO_HALFWAY_CHARGED = 0.75f
    const val TIME_TO_FULLY_CHARGED = 1.5f

    const val SHOOT_ANIM_TIME = 0.3f

    const val DMG_X = 8f
    const val DMG_Y = 5f

    const val EXPLOSION_ORB_SPEED = 3.5f

    const val BULLET_VEL = 10f

    const val ICE_CUBE_VEL = 8f

    const val FIRE_BALL_Y_VEL = 7.5f
    const val FIRE_BALL_X_VEL = 10f
    const val FIRE_BALL_GRAVITY = -0.25f

    const val MOON_SCYTHE_SPEED = 8f
    const val MAX_MOONS_BEFORE_SHOOT_AGAIN = 1
    val MOON_SCYTHE_DEG_OFFSETS = gdxArrayOf(10f, 40f, 70f)

    const val SPAWNING_DUR = 0.05f

    const val CROUCH_DELAY = 0.1f

    const val STUN_DUR = 0.25f
    const val STUN_IMPULSE_X = 2.5f
    const val STUM_IMPULSE_Y = 2f

    const val HITS_TO_UNFREEZE = 3
    const val FROZEN_PUSH_DUR = 0.2f
    val BUTTONS_TO_UNFREEZE = gdxArrayOf<Any>(MegaControllerButton.B)
}
