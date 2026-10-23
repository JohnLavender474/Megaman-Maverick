package com.maverick.game.engine.players

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.entities.IGameEntity

interface IPlayers<out V : PlayerView> {

    val capacity: Int

    fun get(slot: PlayerSlot): V?
}

inline fun <V : PlayerView> IPlayers<V>.forEach(action: (V) -> Unit) {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let(action)
}

inline fun <V : PlayerView> IPlayers<V>.any(predicate: (V) -> Boolean): Boolean {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let { if (predicate(it)) return true }
    return false
}

inline fun <V : PlayerView> IPlayers<V>.all(predicate: (V) -> Boolean): Boolean {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let { if (!predicate(it)) return false }
    return true
}

fun <V : PlayerView> IPlayers<V>.viewOf(entity: Any?): V? {
    for (i in 0 until capacity) {
        val view = get(PlayerSlot(i)) ?: continue
        if (view.entity === entity) return view
    }
    return null
}

fun IPlayers<*>.slotOf(entity: IGameEntity): PlayerSlot? = viewOf(entity)?.slot

fun IPlayers<*>.registeredSlots(out: Array<PlayerSlot> = Array()): Array<PlayerSlot> {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let { out.add(it.slot) }
    return out
}
