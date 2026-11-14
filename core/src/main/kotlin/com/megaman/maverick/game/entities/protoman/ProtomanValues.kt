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
    const val ART_FRAME_PX = 48
    const val AIR_SPRITE_OFFSET_Y = -0.25f

    const val STAND_OPEN_TIME = 1.5f
    const val STAND_FLICK_TIME = 0.15f
    const val STEP_TIME = 0.1f
    const val RUN_FRAME_TIME = 0.125f

    const val SHOT_SPEED = 12f
    const val SHOT_WAIT = 0.1f
    const val SHOT_POSE_TIME = 0.3f

    const val SHOT_OFFSET_X = 1f
    const val SHOT_OFFSET_Y = 0f
    const val SHOT_RUN_OFFSET_X = 0.5f
    const val SHOT_AIR_OFFSET_Y = 0.1f
    const val STAND_SHOOT_MUZZLE_X = 43
    const val STAND_SHOOT_MUZZLE_Y = 35
    const val CROUCH_SHOOT_MUZZLE_X = 40
    const val CROUCH_SHOOT_MUZZLE_Y = 38
    const val SHOT_CROUCH_OFFSET_X =
        SHOT_OFFSET_X + (CROUCH_SHOOT_MUZZLE_X - STAND_SHOOT_MUZZLE_X) * SPRITE_SIZE / ART_FRAME_PX
    const val SHOT_CROUCH_OFFSET_Y =
        SHOT_OFFSET_Y - (CROUCH_SHOOT_MUZZLE_Y - STAND_SHOOT_MUZZLE_Y) * SPRITE_SIZE / ART_FRAME_PX

    const val GUARD_TURN_FRAME_TIME = 0.05f
    const val GUARD_CROUCH_TURN_FRAMES = 4
    const val GUARD_JUMP_TURN_FRAMES = 3
    const val GUARD_CROUCH_PULL_OUT_TIME = GUARD_CROUCH_TURN_FRAMES * GUARD_TURN_FRAME_TIME
    const val GUARD_JUMP_PULL_OUT_TIME = GUARD_JUMP_TURN_FRAMES * GUARD_TURN_FRAME_TIME
    const val GUARD_CROUCH_PUT_AWAY_TIME = GUARD_CROUCH_TURN_FRAMES * GUARD_TURN_FRAME_TIME
    const val GUARD_JUMP_PUT_AWAY_TIME = GUARD_JUMP_TURN_FRAMES * GUARD_TURN_FRAME_TIME
    internal val GUARD_CROUCH_SHIELD_BOX = ArtBox(left = 31, top = 30, right = 37, bottom = 47)
    internal val GUARD_JUMP_SHIELD_BOX = ArtBox(left = 32, top = 22, right = 38, bottom = 39)
    const val GUARD_CROUCH_ART_ROWS = 18
    const val GUARD_CROUCH_HEIGHT = GUARD_CROUCH_ART_ROWS * SPRITE_SIZE / ART_FRAME_PX
    const val DEFLECT_BURST_TIME = 0.15f
}
