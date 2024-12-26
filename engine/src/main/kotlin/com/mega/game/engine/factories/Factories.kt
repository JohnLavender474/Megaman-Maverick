package com.mega.game.engine.factories

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap

open class Factories<T> {

    open val factories = ObjectMap<Any, IFactory<T>>()

    open fun add(factoryKey: Any, factory: IFactory<T>): IFactory<T> =
        factories.put(factoryKey, factory)

    open fun remove(factoryKey: Any): IFactory<T> = factories.remove(factoryKey)

    open fun fetch(factoryKey: Any, objKey: Any) = factories[factoryKey]?.fetch(objKey)

    open fun fetch(factoryKey: Any, objKey: Any, count: Int): Array<T> {
        val factory = factories[factoryKey] ?: throw IllegalArgumentException("Factory not found for key: $factoryKey")
        val objects = Array<T>()
        repeat(count) {
            val obj = factory.fetch(objKey) ?: throw IllegalArgumentException("Object not found for key: $objKey")
            objects.add(obj)
        }
        return objects
    }
}
