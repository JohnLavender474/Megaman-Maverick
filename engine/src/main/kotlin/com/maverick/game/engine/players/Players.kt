package com.maverick.game.engine.players

import com.maverick.game.engine.entities.IGameEntity

open class Players<V : PlayerView>(
    final override val capacity: Int,
    private val createView: (PlayerSlot, IGameEntity) -> V
) : IPlayers<V> {

    private val views = arrayOfNulls<PlayerView>(capacity)

    init {
        if (capacity < 1) throw IllegalArgumentException("Capacity must be at least 1: $capacity")
    }

    fun register(slot: PlayerSlot, entity: IGameEntity): V {
        checkInRange(slot)
        if (views[slot.index] != null) throw IllegalStateException("$slot already has a player")
        val view = createView(slot, entity)
        views[slot.index] = view
        return view
    }

    fun unregister(slot: PlayerSlot) {
        checkInRange(slot)
        views[slot.index] = null
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(slot: PlayerSlot): V? {
        if (slot.index !in 0 until capacity) return null
        return views[slot.index] as V?
    }

    private fun checkInRange(slot: PlayerSlot) {
        if (slot.index !in 0 until capacity)
            throw IllegalArgumentException("$slot is out of range for capacity $capacity")
    }
}
