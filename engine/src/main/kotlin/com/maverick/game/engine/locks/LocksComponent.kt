package com.maverick.game.engine.locks

import com.maverick.game.engine.components.IGameComponent

class LocksComponent(
    private val groupCount: Int,
    private val onChange: ((group: LockGroup, locked: Boolean) -> Unit)? = null
) : IGameComponent {

    companion object {
        const val MAX_REASONS = 32

        internal fun bit(reason: LockReason): Int {
            val i = reason.index
            if (i !in 0 until MAX_REASONS) throw IllegalArgumentException("$reason has index $i, outside 0 until $MAX_REASONS")
            return 1 shl i
        }
    }

    private val groups = Array(groupCount) { LockGroupState() }

    fun getLockGroup(group: LockGroup) = groups[check(group)]

    fun add(group: LockGroup, reason: LockReason) {
        val state = getLockGroup(group)
        state.reasons = state.reasons or bit(reason)
        onChange?.invoke(group, true)
    }

    fun remove(group: LockGroup, reason: LockReason) {
        val state = getLockGroup(group)
        state.reasons = state.reasons and bit(reason).inv()
        if (state.reasons == 0) onChange?.invoke(group, false)
    }

    fun clear(group: LockGroup) {
        val state = getLockGroup(group)
        val wasLocked = state.isLocked()
        state.reasons = 0
        if (wasLocked) onChange?.invoke(group, false)
    }

    fun isLocked(group: LockGroup) = getLockGroup(group).isLocked()

    override fun reset() = groups.forEach { it.reasons = 0 }

    private fun check(group: LockGroup): Int {
        val i = group.index
        if (i !in 0 until groupCount) throw IllegalArgumentException("$group has index $i, outside 0 until $groupCount")
        return i
    }
}
