package com.mega.game.engine.entities

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.GameEngine
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.components.IGameComponent
import kotlin.reflect.KClass
import kotlin.reflect.cast

abstract class GameEntity(
    val engine: GameEngine,
    override val components: OrderedMap<KClass<out IGameComponent>, IGameComponent> = OrderedMap(),
    override val properties: Properties = Properties()
) : IGameEntity {

    companion object {
        const val TAG = "GameEntity"
    }

    override var initialized = false

    override var spawned = false

    override fun init() {}

    fun spawn(spawnProps: Properties) = engine.spawn(this, spawnProps)

    fun destroy() = engine.destroy(this)

    override fun canSpawn(spawnProps: Properties) = true

    override fun <C : IGameComponent> getComponent(key: KClass<C>): C? {
        val value = components[key] ?: return null
        return key.cast(value)
    }

    override fun addComponent(c: IGameComponent) {
        components.put(c::class, c)
    }

    override fun hasComponent(key: KClass<out IGameComponent>) = components.containsKey(key)

    override fun removeComponent(key: KClass<out IGameComponent>) {
        components.remove(key)
    }

    override fun clearComponents() = components.clear()
}
