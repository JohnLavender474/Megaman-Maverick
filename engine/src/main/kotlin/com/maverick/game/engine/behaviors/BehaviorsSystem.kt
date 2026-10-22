package com.maverick.game.engine.behaviors

import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem


class BehaviorsSystem(
    private val diagnostics: RuntimeDiagnostics? = null
) : GameSystem(BehaviorsComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("BehaviorsSystem")

        entities.forEach { entity ->
            try {
                val behaviorsComponent = entity.getComponent(BehaviorsComponent::class)!!
                behaviorsComponent.behaviors.forEach { entry ->
                    val key = entry.key
                    if (behaviorsComponent.isBehaviorAllowed(key)) {
                        val behavior = entry.value
                        behavior.update(delta)
                    }
                }
            } catch (e: Exception) {
                throw Exception("Exception occured while processing behaviors for entity: $entity", e)
            }
        }

        diagnostics?.endEntry()
    }
}
