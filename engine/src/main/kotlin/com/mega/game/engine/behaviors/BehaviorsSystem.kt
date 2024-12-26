package com.mega.game.engine.behaviors

import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem


class BehaviorsSystem : GameSystem(BehaviorsComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return
        entities.forEach { entity ->
            val behaviorsComponent = entity.getComponent(BehaviorsComponent::class)!!
            behaviorsComponent.behaviors.forEach { entry ->
                val key = entry.key
                if (behaviorsComponent.isBehaviorAllowed(key)) {
                    val behavior = entry.value
                    behavior.update(delta)
                }
            }
        }
    }
}
