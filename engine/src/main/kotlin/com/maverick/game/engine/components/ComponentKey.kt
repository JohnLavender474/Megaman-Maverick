package com.maverick.game.engine.components

import kotlin.reflect.KClass

class ComponentKey<C : IGameComponent>(val type: KClass<C>) {

    override fun toString() = "ComponentKey(${type.simpleName})"
}

operator fun <C : IGameComponent> IComponentBucket.get(key: ComponentKey<C>): C? = getComponent(key.type)

fun <C : IGameComponent> IComponentBucket.require(type: KClass<C>): C =
    getComponent(type) ?: throw IllegalStateException("Entity $this has no ${type.simpleName}")
