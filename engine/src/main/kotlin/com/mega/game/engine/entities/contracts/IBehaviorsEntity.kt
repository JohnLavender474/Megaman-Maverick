package com.mega.game.engine.entities.contracts

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.behaviors.BehaviorsComponent
import com.mega.game.engine.behaviors.IBehavior
import com.mega.game.engine.entities.IGameEntity
import java.util.stream.StreamSupport

interface IBehaviorsEntity : IGameEntity {

    val behaviorsComponent: BehaviorsComponent
        get() {
            val key = BehaviorsComponent::class
            return getComponent(key)!!
        }

    val behaviors: OrderedMap<Any, IBehavior>
        get() = this.behaviorsComponent.behaviors

    fun isBehaviorActive(key: Any): Boolean {
        return this.behaviorsComponent.isBehaviorActive(key)
    }

    fun getBehavior(key: Any?): IBehavior? {
        return this.behaviors.get<Any?>(key)
    }

    fun isAnyBehaviorActive(vararg keys: Any): Boolean {
        return isAnyBehaviorActive(listOf<Any>(*keys))
    }

    fun isAnyBehaviorActive(keys: Iterable<Any>): Boolean {
        return StreamSupport.stream<Any>(keys.spliterator(), false)
            .anyMatch { key: Any -> this.isBehaviorActive(key) }
    }

    fun areAllBehaviorsActive(vararg keys: Any): Boolean {
        return areAllBehaviorsActive(listOf<Any>(*keys))
    }

    fun areAllBehaviorsActive(keys: Iterable<Any?>): Boolean {
        return StreamSupport.stream<Any?>(keys.spliterator(), false)
            .allMatch { key: Any? -> this.isBehaviorActive(key!!) }
    }

    fun resetBehavior(key: Any) {
        val behavior = getBehavior(key)
        behavior?.reset()
    }

    fun isBehaviorAllowed(key: Any): Boolean {
        return this.behaviorsComponent.isBehaviorAllowed(key)
    }

    fun setBehaviorAllowed(key: Any, allowed: Boolean) {
        this.behaviorsComponent.setBehaviorAllowed(key, allowed)
    }

    fun setBehaviorsAllowed(function: (Any, IBehavior) -> Boolean) {
        this.behaviorsComponent.setBehaviorsAllowed(function)
    }

    fun setBehaviorsAllowed(keys: Iterable<Any>, allowed: Boolean) {
        this.behaviorsComponent.setBehaviorsAllowed(keys, allowed)
    }

    fun setAllBehaviorsAllowed(allowed: Boolean) {
        this.behaviorsComponent.setAllBehaviorsAllowed(allowed)
    }
}

