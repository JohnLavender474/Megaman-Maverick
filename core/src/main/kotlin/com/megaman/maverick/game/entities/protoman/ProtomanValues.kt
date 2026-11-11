package com.megaman.maverick.game.entities.protoman

object ProtomanValues {

    const val BODY_WIDTH = 1f
    const val BODY_HEIGHT = 1.5f

    const val CLAMP_X = 16f
    const val CLAMP_Y = 40f

    const val RUN_IMPULSE = 40f
    const val ICE_RUN_IMPULSE = 15f
    const val RUN_1_TIME = 0.1f
    const val RUN_1_SCALAR = 0.75f
    const val RUN_2_TIME = 0.25f
    const val RUN_2_SCALAR = 0.9f
    const val RUN_3_SCALAR = 1f
    const val RUN_MAX_SPEED = 6f
    const val WATER_RUN_MAX_SPEED_SCALAR = 0.5f

    const val POST_ACTION_MOMENTUM_DUR = 0.35f
    const val POST_ACTION_BURST_RESISTANCE = 16f

    const val JUMP_VEL = 25f

    const val GROUND_GRAVITY = -0.15f
    const val JUMP_GRAVITY = -30f
    const val FALL_GRAVITY = -75f
    const val ICE_GRAVITY = -112.5f
    const val WATER_GRAVITY = -37.5f
    const val WATER_ICE_GRAVITY = -75f
    const val WATER_GRAVITY_SCALAR = 0.5f

    const val SPRITE_SIZE = 3f
    const val AIR_SPRITE_OFFSET_Y = -0.25f

    const val STAND_OPEN_TIME = 1.5f
    const val STAND_FLICK_TIME = 0.15f
    const val STEP_TIME = 0.1f
    const val RUN_FRAME_TIME = 0.125f
}
