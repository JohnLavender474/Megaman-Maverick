package com.megaman.maverick.game.locks

import com.maverick.game.engine.locks.LockReason

enum class MegaLockReason(override val index: Int) : LockReason {
    SPAWN(0),
    TELEPORT(1),
    CAMERA_ROTATION(2),
    BOSS_DEFEATED(3),
    LEVEL_END(4),
    WILY_FINAL_BOSS(5),
    SPIDER_WEB(6)
}
