package com.megaman.maverick.game.locks

import com.maverick.game.engine.locks.LockGroup

enum class MegaLockGroup(override val index: Int) : LockGroup {
    DAMAGE(0),
    MOVEMENT(1),
    BEHAVIORS(2)
}
