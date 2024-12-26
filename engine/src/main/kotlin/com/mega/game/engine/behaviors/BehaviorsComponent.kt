package com.mega.game.engine.behaviors

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.components.IGameComponent
import java.util.function.BiFunction


class BehaviorsComponent() : IGameComponent {

    val behaviors = OrderedMap<Any, IBehavior>()
    val allowedBehaviors = ObjectMap<Any, Boolean>()


    constructor(vararg _behaviors: GamePair<Any, IBehavior>) : this() {
        _behaviors.forEach { addBehavior(it.first, it.second) }
    }


    constructor(_behaviors: Iterable<GamePair<Any, IBehavior>>) : this() {
        _behaviors.forEach { addBehavior(it.first, it.second) }
    }


    constructor(_behaviors: OrderedMap<Any, IBehavior>) : this() {
        _behaviors.forEach { addBehavior(it.key, it.value) }
    }


    fun getBehavior(key: Any): IBehavior? = behaviors.get(key)


    fun addBehavior(key: Any, behavior: IBehavior) {
        behaviors.put(key, behavior)
        allowedBehaviors.put(key, true)
    }


    fun setBehaviorAllowed(key: Any, allowed: Boolean) {
        if (!behaviors.containsKey(key)) throw IllegalArgumentException("Key must be associated to an already added behavior")
        allowedBehaviors.put(key, allowed)
        if (!allowed) {
            val behavior = behaviors.get(key)
            if (behavior.isActive()) behavior.end()
        }
    }


    fun setBehaviorsAllowed(function: (Any, IBehavior) -> Boolean) {
        behaviors.forEach { entry ->
            val key = entry.key
            val behavior = entry.value
            val allowed = function.invoke(key, behavior)
            setBehaviorAllowed(key, allowed)
        }
    }


    fun setBehaviorsAllowed(function: BiFunction<Any, IBehavior, Boolean>) {
        setBehaviorsAllowed { key, behavior -> function.apply(key, behavior) }
    }


    fun setBehaviorsAllowed(keys: Iterable<Any>, allowed: Boolean) {
        keys.forEach { key ->
            setBehaviorAllowed(key, allowed)
        }
    }


    fun setAllBehaviorsAllowed(allowed: Boolean) {
        behaviors.forEach { entry ->
            val key = entry.key
            setBehaviorAllowed(key, allowed)
        }
    }


    fun isBehaviorAllowed(key: Any): Boolean = allowedBehaviors.containsKey(key) && allowedBehaviors.get(key)


    fun isBehaviorActive(key: Any) = behaviors.get(key)?.isActive() ?: false


    fun isAnyBehaviorActive(vararg keys: Any) = isAnyBehaviorActive(keys.asIterable())


    fun isAnyBehaviorActive(keys: Iterable<Any>) = keys.any { isBehaviorActive(it) }


    fun areAllBehaviorsActive(vararg keys: Any) = areAllBehaviorsActive(keys.asIterable())


    fun areAllBehaviorsActive(keys: Iterable<Any>) = keys.all { isBehaviorActive(it) }


    override fun reset() = behaviors.forEach {
        if (isBehaviorActive(it.key)) it.value.reset()
    }
}
