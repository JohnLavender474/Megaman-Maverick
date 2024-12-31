package com.mega.game.engine.factories

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap

open class Factories<K, V> {

    open val factories = ObjectMap<Any, IFactory<K, V>>()

    open fun add(factoryKey: K, factory: IFactory<K, V>): IFactory<K, V> =
        factories.put(factoryKey, factory)

    open fun remove(factoryKey: K): IFactory<K, V> = factories.remove(factoryKey)

    open fun fetch(factoryKey: K, objKey: K) = factories[factoryKey]?.fetch(objKey)

    open fun fetch(factoryKey: K, objKey: K, count: Int): Array<V> {
        val factory = factories[factoryKey] ?: throw IllegalArgumentException("Factory not found for key: $factoryKey")
        val objects = Array<V>()
        repeat(count) {
            val obj = factory.fetch(objKey) ?: throw IllegalArgumentException("Object not found for key: $objKey")
            objects.add(obj)
        }
        return objects
    }
}
