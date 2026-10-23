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

inline fun <V : PlayerView> IPlayers<V>.forEachPresent(action: (V) -> Unit) {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let { if (it.present) action(it) }
}

inline fun <V : PlayerView> IPlayers<V>.any(predicate: (V) -> Boolean): Boolean {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let { if (predicate(it)) return true }
    return false
}

inline fun <V : PlayerView> IPlayers<V>.all(predicate: (V) -> Boolean): Boolean {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let { if (!predicate(it)) return false }
    return true
}

inline fun <V : PlayerView> IPlayers<V>.firstOrNull(predicate: (V) -> Boolean): V? {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let { if (predicate(it)) return it }
    return null
}

inline fun <V : PlayerView> IPlayers<V>.minByOrNull(selector: (V) -> Float): V? {
    var best: V? = null
    var bestValue = 0f
    for (i in 0 until capacity) {
        val view = get(PlayerSlot(i)) ?: continue
        val value = selector(view)
        if (best == null || value < bestValue) {
            best = view
            bestValue = value
        }
    }
    return best
}

fun IPlayers<*>.slotOf(entity: IGameEntity): PlayerSlot? {
    for (i in 0 until capacity) {
        val view = get(PlayerSlot(i)) ?: continue
        if (view.entity === entity) return view.slot
    }
    return null
}

fun IPlayers<*>.registeredSlots(out: Array<PlayerSlot> = Array()): Array<PlayerSlot> {
    for (i in 0 until capacity) get(PlayerSlot(i))?.let { out.add(it.slot) }
    return out
}
