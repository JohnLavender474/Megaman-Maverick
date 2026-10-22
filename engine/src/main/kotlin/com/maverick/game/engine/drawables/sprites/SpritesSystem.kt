package com.maverick.game.engine.drawables.sprites

import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem

class SpritesSystem(
    private val collector: (GameSprite) -> Unit,
    private val diagnostics: RuntimeDiagnostics? = null
) :
    GameSystem(SpritesComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("SpritesSystem")

        entities.forEach { entity ->
            try {
                val component = entity.getComponent(SpritesComponent::class)!!
                if (component.doUpdateAndDraw.invoke(delta)) {
                    component.preProcess(delta)
                    component.sprites.values().forEach { sprite -> collector.invoke(sprite) }
                    component.postProcess(delta)
                }
            } catch (e: Exception) {
                throw Exception("Exception occured while processing sprites for entity: $entity", e)
            }
        }

        diagnostics?.endEntry()
    }
}
