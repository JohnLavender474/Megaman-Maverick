package com.mega.game.engine.animations

import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem

class AnimationsSystem : GameSystem(AnimationsComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        for (entity in entities) {
            val component = entity.getComponent(AnimationsComponent::class)
            component?.animators?.forEach { e ->
                val animator = e.value
                if (!animator.shouldAnimate(delta)) return@forEach

                val key = e.key

                val sprite = component.sprites[key]
                if (sprite == null) return@forEach

                animator.animate(sprite, delta)
            }
        }
    }
}
