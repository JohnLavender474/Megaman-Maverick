package com.maverick.game.engine.points

import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem

class PointsSystem(
    private val diagnostics: RuntimeDiagnostics? = null
) : GameSystem(PointsComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("PointsSystem")

        entities.forEach { entity ->
            try {
                val pointsComponent = entity.getComponent(PointsComponent::class)
                pointsComponent?.pointsListeners?.forEach { e ->
                    val key = e.key
                    val listener = e.value

                    val points = pointsComponent.pointsMap[key]
                    if (points != null) listener.invoke(points)
                }
            } catch (e: Exception) {
                throw Exception("Exception occured while processing points for entity: $entity", e)
            }
        }

        diagnostics?.endEntry()
    }
}
