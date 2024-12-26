package com.mega.game.engine.updatables

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Wrapper
import com.mega.game.engine.components.IGameComponent
import java.util.*

class UpdatablesComponent(val updatables: OrderedMap<Any, Updatable> = OrderedMap()) : IGameComponent {

    constructor(vararg updatables: Updatable) : this(OrderedMap<Any, Updatable>().apply {
        updatables.forEach {
            val key = UUID.randomUUID().toString()
            put(key, it)
        }
    })

    fun add(updatable: Updatable): String {
        val key = UUID.randomUUID().toString()
        put(key, updatable)
        return key
    }

    fun put(key: Any, updatable: Updatable): Updatable? = updatables.put(key, updatable)

    fun remove(key: Any): Updatable? = updatables.remove(key)

    fun contains(key: Any) = updatables.containsKey(key)
}

class UpdatablesComponentBuilder {

    private val updatables = OrderedMap<Any, Updatable>()

    fun add(updatable: Updatable, out: Wrapper<String>? = null): UpdatablesComponentBuilder {
        val key = UUID.randomUUID().toString()
        updatables.put(key, updatable)
        out?.data = key
        return this
    }

    fun put(key: Any, updatable: Updatable): UpdatablesComponentBuilder {
        updatables.put(key, updatable)
        return this
    }

    fun build() = UpdatablesComponent(updatables)
}
