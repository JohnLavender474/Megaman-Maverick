package com.maverick.game.engine.common.interfaces

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Predicate
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.PropertyKey
import kotlin.reflect.KClass
import kotlin.reflect.cast

interface IPropertizable {

    val properties: Properties

    fun putProperty(key: Any, p: Any?) = properties.put(key, p)

    fun putAllProperties(p: ObjectMap<Any, Any?>) = properties.putAll(p)

    fun putAllProperties(p: Properties) = properties.putAll(p)

    fun getProperty(key: Any) = properties.get(key)

    fun getAllMatchingProperties(keyPredicate: Predicate<Any>) = properties.getAllMatching(keyPredicate)

    fun getAllMatchingProperties(keyPredicate: (Any) -> Boolean) = properties.getAllMatching(keyPredicate)

    fun <T : Any> getProperty(key: Any, type: KClass<T>) = properties.get(key, type)

    fun <T : Any> getProperty(key: Any, type: Class<T>) = properties.get(key, type)

    fun getOrDefaultProperty(key: Any, default: Any) = properties.getOrDefault(key, default)

    fun <T : Any> getOrDefaultProperty(key: Any, default: T, type: KClass<T>) =
        properties.getOrDefault(key, default, type)

    fun <T : Any> getOrDefaultProperty(key: Any, default: T, type: Class<T>): T =
        properties.getOrDefault(key, default, type)

    fun hasProperty(key: Any) = properties.containsKey(key)

    fun isProperty(key: Any, value: Any) = properties.isProperty(key, value)

    fun removeProperty(key: Any) = properties.remove(key)

    fun <T : Any> removeProperty(key: Any, type: KClass<T>): T? {
        val value = properties.remove(key)
        return if (value != null) type.cast(value) else null
    }

    fun clearProperties() = properties.clear()

    fun <T : Any> putProperty(key: PropertyKey<T>, value: T) = properties.put(key, value)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getProperty(key: PropertyKey<T>) = properties.get(key) as T?

    fun hasProperty(key: PropertyKey<*>) = properties.containsKey(key)

    fun removeProperty(key: PropertyKey<*>) = properties.remove(key)
}
