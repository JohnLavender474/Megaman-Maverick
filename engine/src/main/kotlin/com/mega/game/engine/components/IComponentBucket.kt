package com.mega.game.engine.components

import com.badlogic.gdx.utils.Array
import kotlin.reflect.KClass

interface IComponentBucket {

    fun <C : IGameComponent> getComponent(key: KClass<C>): C?

    fun addComponent(c: IGameComponent)

    fun addComponents(components: Array<IGameComponent>) = components.forEach { addComponent(it) }

    fun hasComponent(key: KClass<out IGameComponent>): Boolean

    fun removeComponent(key: KClass<out IGameComponent>)

    fun clearComponents()
}