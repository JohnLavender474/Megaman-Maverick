package com.mega.game.engine.common.extensions

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

fun <K, V> ObjectMap<K, V>.putIfAbsentAndGet(key: K, defaultValueSupplier: () -> V): V {
    if (!containsKey(key)) put(key, defaultValueSupplier.invoke())
    return get(key)
}

fun <K, V> ObjectMap<K, V>.putAll(vararg entries: GamePair<K, V>) = entries.forEach { put(it.first, it.second) }
