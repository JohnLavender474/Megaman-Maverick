package com.maverick.game.engine.animations

import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem

class AnimationsSystem(
    private val diagnostics: RuntimeDiagnostics? = null
) : GameSystem(AnimationsComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("AnimationsSystem")

        for (entity in entities) {
            try {
                val component = entity.getComponent(AnimationsComponent::class)!!
                if (component.shouldAnimate.invoke(delta)) component.animators.forEach { e ->
                    val animator = e.value
                    if (!animator.shouldAnimate(delta)) return@forEach

                    val key = e.key

                    val sprite = component.sprites[key] ?: return@forEach
                    animator.animate(sprite, delta)
                }
            } catch (e: Exception) {
                throw Exception("Exception occured while processing animations for entity: $entity", e)
            }
        }

        diagnostics?.endEntry()
    }
}
