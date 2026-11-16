package com.maverick.game.engine.locks

class LockGroupState internal constructor() {

    internal var reasons = 0

    internal var group: LockGroup? = null

    fun isLocked() = reasons != 0

    fun isLockedBy(reason: LockReason) = reasons and LocksComponent.bit(reason) != 0
}
