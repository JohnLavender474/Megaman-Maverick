package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.ObjectMap


class ImmutableMap<K, V>(private val map: ObjectMap<K, V>) : Map<K, V> {

    class MapEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

    override val entries: Set<Map.Entry<K, V>>
        get() {
            val entries = HashSet<MapEntry<K, V>>()
            map.entries().forEach { entries.add(MapEntry(it.key, it.value)) }
            return entries
        }

    override val keys: Set<K>
        get() {
            val set = HashSet<K>()
            map.keys().forEach { set.add(it) }
            return set
        }

    override val values: Collection<V>
        get() {
            val values = ArrayList<V>()
            map.values().forEach { values.add(it) }
            return values
        }

    override val size: Int
        get() = map.size


    fun iterator(): ImmutableIterator<ObjectMap.Entry<K, V>> = ImmutableIterator(map.iterator())

    override fun containsValue(value: V) = map.values().contains(value)

    override fun containsKey(key: K) = map.containsKey(key)

    override fun isEmpty() = map.isEmpty

    override fun equals(other: Any?) = map == other

    override fun get(key: K): V? = map[key]

    override fun hashCode() = map.hashCode()

    override fun toString() = "ImmutableMap(map=$map)"
}