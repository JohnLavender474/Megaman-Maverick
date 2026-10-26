package com.maverick.game.engine.players

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.entities.IGameEntity

interface IPlayers<out E : IGameEntity> {

    val capacity: Int

    fun get(slot: PlayerSlot): E?
}

inline fun <E : IGameEntity> IPlayers<E>.forEach(action: (E) -> Unit) {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let(action)
}

inline fun <E : IGameEntity> IPlayers<E>.any(predicate: (E) -> Boolean): Boolean {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let { if (predicate(it)) return true }
    return false
}

inline fun <E : IGameEntity> IPlayers<E>.all(predicate: (E) -> Boolean): Boolean {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let { if (!predicate(it)) return false }
    return true
}

fun IPlayers<*>.registeredSlots(out: Array<PlayerSlot> = Array()): Array<PlayerSlot> {
    for (i in 0 until capacity) if (get(PlayerSlot(i)) != null) out.add(PlayerSlot(i))
    return out
}
