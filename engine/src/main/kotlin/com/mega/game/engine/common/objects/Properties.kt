package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Predicate
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.mega.game.engine.common.interfaces.ICopyable
import kotlin.reflect.KClass
import kotlin.reflect.cast

fun props(vararg pairs: GamePair<Any, Any?>) = Properties().apply { pairs.forEach { put(it.first, it.second) } }

class Properties : ICopyable<Properties> {

    val size: Int
        get() = map.size

    private val map: ObjectMap<Any, Any?>

    constructor() {
        map = ObjectMap()
    }

    constructor(initialCapacity: Int, loadFactor: Float) {
        map = ObjectMap(initialCapacity, loadFactor)
    }

    constructor(initialCapacity: Int) {
        map = ObjectMap(initialCapacity)
    }

    constructor(m: ObjectMap<Any, Any?>) : this() {
        m.forEach { put(it.key, it.value) }
    }

    constructor(props: Properties) : this(props.map)

    fun <T : Any> get(key: Any, type: KClass<T>) = map.get(key)?.let { type.cast(it) }

    fun <T : Any> get(key: Any, type: Class<T>) = map.get(key)?.let { type.cast(it) }

    fun put(key: Any, value: Any?) = map.put(key, value)

    fun putIfAbsent(key: Any, value: Any?): Any? {
        val old = map.get(key)
        if (old != null) return old

        map.put(key, value)
        return value
    }

    fun putIfAbsentAndGet(key: Any, defaultValue: Any?) = map.putIfAbsentAndGet(key, defaultValue)

    fun clear() = map.clear()

    fun isEmpty() = map.isEmpty

    fun remove(key: Any) = map.remove(key)

    fun putAll(from: ObjectMap<Any, Any?>) = map.putAll(from)

    fun putAll(_props: Properties) = _props.forEach { key, value -> put(key, value) }

    fun <K : Any, V> putAll(vararg _props: GamePair<K, V>) = _props.forEach { put(it.first, it.second) }

    fun get(key: Any) = map.get(key)

    fun isProperty(key: Any, value: Any) = map.get(key) == value

    fun getAllMatching(keyPredicate: (Any) -> Boolean): Array<GamePair<Any, Any?>> {
        val matching = Array<GamePair<Any, Any?>>()
        forEach { key, value -> if (keyPredicate(key)) matching.add(GamePair(key, value)) }
        return matching
    }

    fun getAllMatching(keyPredicate: Predicate<Any>): Array<GamePair<Any, Any?>> {
        val matching = Array<GamePair<Any, Any?>>()
        forEach { key, value -> if (keyPredicate.evaluate(key)) matching.add(GamePair(key, value)) }
        return matching
    }

    fun getOrDefault(key: Any, defaultValue: Any?) = if (containsKey(key)) get(key) else defaultValue

    fun <T : Any> getOrDefault(key: Any, defaultValue: Any, type: KClass<T>) =
        type.cast(if (containsKey(key)) get(key, type) else defaultValue)

    fun <T : Any> getOrDefault(key: Any, defaultValue: Any, type: Class<T>): T =
        type.cast(if (containsKey(key)) get(key, type) else defaultValue)

    fun <T : Any> getOrDefaultNotNull(key: Any, defaultValue: Any, type: KClass<T>): T {
        val value = get(key)
        return if (value != null) type.cast(value) else type.cast(defaultValue)
    }

    fun <T : Any> getOrDefaultNotNull(key: Any, defaultValue: Any, type: Class<T>): T {
        val value = get(key)
        return if (value != null) type.cast(value) else type.cast(defaultValue)
    }

    fun getNotNull(key: Any) = map.get(key)!!

    fun containsKey(key: Any) = map.containsKey(key)

    fun forEach(action: (key: Any, value: Any?) -> Unit) {
        for (entry in map.entries()) action(entry.key, entry.value)
    }

    fun collect(predicate: (key: Any, value: Any?) -> Boolean): Properties {
        val collected = Properties()
        forEach { key, value -> if (predicate(key, value)) collected.put(key, value) }
        return collected
    }

    override fun copy() = Properties(map)

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Properties({")
        forEach { key, value -> sb.append("[$key=$value]") }
        sb.append("})")
        return sb.toString()
    }
}
