package com.megaman.maverick.game.entities.megamanx

object MegamanXValues {

    const val BODY_WIDTH = 1f
    const val BODY_HEIGHT = 1.5f

    const val SPRITE_SIZE = 3f

    const val WALK_START_SPEED = 1f
    const val WALK_START_TIME = 0.1f
    const val WALK_SPEED = 5f

    const val JUMP_SPEED = 12f
    const val GRAVITY = -30f
    const val GROUND_GRAVITY = -0.15f
    const val FALL_MAX = 12f
    const val CLAMP_X = 16f

    const val TIME_TO_HALF_CHARGE = 0.5f
    const val TIME_TO_FULL_CHARGE = 1.5f
    const val CHARGE_FLASH_TIME = 0.05f

    const val SHOT_SPEED = 15f
    const val HALF_SHOT_SPEED = 15f
    const val FULL_SHOT_SPEED = 18.75f

    const val SHOT_LIMIT = 4
    const val FULL_SHOT_LIMIT = 2

    const val SHOT_WAIT = 0.1f
    const val CHARGED_SHOT_WAIT = 0.25f

    const val SHOT_POSE_TIME = 0.333f
    const val HALF_SHOT_POSE_TIME = 0.4f
    const val FULL_SHOT_POSE_TIME = 0.5f

    const val SHOT_OFFSET_X = 0.75f
    const val SHOT_OFFSET_Y = 0.1f

    const val WALL_GRAB_TIME = 0.1333f
    const val WALL_SLIDE_SPEED = 2.6953f
    const val WALL_KICK_SPEED = 10.5469f
    const val WALL_KICK_AWAY_SPEED = WALK_SPEED
    const val WALL_KICK_LOCK_TIME = 0.1667f
    const val WALL_DELAY_AFTER_JUMP = 0.1667f
    const val PUSH_OFF_SPEED = WALK_SPEED
    const val PUSH_OFF_TIME = 0.0667f

    const val AIR_DASH_HOVER_TIME = 0.1f
    const val AIR_DASH_TIME = 0.3f
    const val AIR_DASH_SPEED = 8f

    const val KNOCKBACK_X = 8f
    const val KNOCKBACK_Y = 5f
    const val KNOCKBACK_DRAG = 6f

    const val DASH_START_DELAY = 0.0833f
    const val DASH_START_SPEED = 12.5f
    const val DASH_SPEED = 8f
    const val DASH_DECEL = 21.1f
    const val DASH_TIME = 0.5f
    const val DASH_END_FRAME_1 = 0.05f
    const val DASH_END_FRAME_2 = 0.0333f
    const val DASH_END_FRAME_3 = 0.1167f
    const val DASH_END_FRAME_4 = 0.0833f
    const val DASH_END_TIME = DASH_END_FRAME_1 + DASH_END_FRAME_2 + DASH_END_FRAME_3 + DASH_END_FRAME_4
    const val DASH_AIR_SPEED = 8f
    const val DASH_WALL_KICK_AWAY_SPEED = 8f
    const val AIR_DASH_FAST_START_SPEED = 12.5f
}
