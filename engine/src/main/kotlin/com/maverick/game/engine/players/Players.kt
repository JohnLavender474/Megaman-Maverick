package com.maverick.game.engine.players

import com.maverick.game.engine.entities.IGameEntity

open class Players<E : IGameEntity>(final override val capacity: Int) : IPlayers<E> {

    private val entities = arrayOfNulls<IGameEntity>(capacity)

    init {
        if (capacity < 1) throw IllegalArgumentException("Capacity must be at least 1: $capacity")
    }

    open fun register(slot: PlayerSlot, entity: E): E {
        checkInRange(slot)
        if (entities[slot.index] != null) throw IllegalStateException("$slot already has a player")
        entity.addComponent(PlayerComponent(slot))
        entities[slot.index] = entity
        return entity
    }

    open fun unregister(slot: PlayerSlot) {
        checkInRange(slot)
        entities[slot.index]?.removeComponent(PlayerComponent::class)
        entities[slot.index] = null
    }

    @Suppress("UNCHECKED_CAST")
    override fun get(slot: PlayerSlot): E? {
        if (slot.index !in 0 until capacity) return null
        return entities[slot.index] as E?
    }

    private fun checkInRange(slot: PlayerSlot) {
        if (slot.index !in 0 until capacity)
            throw IllegalArgumentException("$slot is out of range for capacity $capacity")
    }
}
