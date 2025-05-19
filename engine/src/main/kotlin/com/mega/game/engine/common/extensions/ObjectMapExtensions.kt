package com.mega.game.engine.common.extensions

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.objects.GamePair

fun <T, U> objectMapOf(vararg pairs: GamePair<T, U>): ObjectMap<T, U> {
    val map = ObjectMap<T, U>()
    pairs.forEach { map.put(it.first, it.second) }
    return map
}

fun <T, U> orderedMapOf(vararg pairs: GamePair<T, U>): OrderedMap<T, U> {
    val map = OrderedMap<T, U>()
    pairs.forEach { map.put(it.first, it.second) }
    return map
}

fun <T, U> orderedMapOfEntries(entries: Array<GamePair<T, U>>): OrderedMap<T, U> {
    val map = OrderedMap<T, U>()
    entries.forEach { map.put(it.first, it.second) }
    return map
}

fun <K, V> ObjectMap<K, V>.putIfAbsentAndGet(key: K, defaultValueSupplier: () -> V): V {
    if (!containsKey(key)) put(key, defaultValueSupplier.invoke())
    return get(key)
}

fun <K, V> ObjectMap<K, V>.putAll(vararg entries: GamePair<K, V>) = entries.forEach { put(it.first, it.second) }

fun <K, V> OrderedMap<K, V>.forEachIndexed(consumer: (key: K, value: V, index: Int) -> Unit) {
    var index = 0
    this.forEach { entry ->
        consumer.invoke(entry.key, entry.value, index)
        index++
    }
}
