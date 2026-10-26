package com.maverick.game.engine.locks

import com.maverick.game.engine.components.IGameComponent

class LocksComponent(
    private val groupCount: Int,
    private val onChange: ((group: LockGroup, locked: Boolean) -> Unit)? = null
) : IGameComponent {

    companion object {
        const val MAX_REASONS = 32
    }

    private val reasonsByGroup = IntArray(groupCount)

    fun add(group: LockGroup, reason: LockReason) {
        val i = check(group)
        reasonsByGroup[i] = reasonsByGroup[i] or bit(reason)
        onChange?.invoke(group, true)
    }

    fun remove(group: LockGroup, reason: LockReason) {
        val i = check(group)
        reasonsByGroup[i] = reasonsByGroup[i] and bit(reason).inv()
        if (reasonsByGroup[i] == 0) onChange?.invoke(group, false)
    }

    fun clear(group: LockGroup) {
        val i = check(group)
        val wasLocked = reasonsByGroup[i] != 0
        reasonsByGroup[i] = 0
        if (wasLocked) onChange?.invoke(group, false)
    }

    fun isLocked(group: LockGroup) = reasonsByGroup[check(group)] != 0

    fun isLockedBy(group: LockGroup, reason: LockReason) = reasonsByGroup[check(group)] and bit(reason) != 0

    override fun reset() = reasonsByGroup.fill(0)

    private fun check(group: LockGroup): Int {
        val i = group.index
        if (i !in 0 until groupCount) throw IllegalArgumentException("$group has index $i, outside 0 until $groupCount")
        return i
    }

    private fun bit(reason: LockReason): Int {
        val i = reason.index
        if (i !in 0 until MAX_REASONS) throw IllegalArgumentException("$reason has index $i, outside 0 until $MAX_REASONS")
        return 1 shl i
    }
}
