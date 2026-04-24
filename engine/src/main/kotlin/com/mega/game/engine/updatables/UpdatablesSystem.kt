package com.mega.game.engine.updatables

import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.diagnostics.RuntimeDiagnostics
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem

class UpdatablesSystem(
    private val diagnostics: RuntimeDiagnostics? = null
) : GameSystem(UpdatablesComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("UpdatablesSystem")

        entities.forEach { entity ->
            try {
                val component = entity.getComponent(UpdatablesComponent::class)
                component?.updatables?.values()?.forEach { it.update(delta) }
            } catch (e: Exception) {
                throw Exception("Exception occured while processing updatables for entity: $entity", e)
            }
        }

        diagnostics?.endEntry()
    }
}
