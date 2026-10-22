package com.maverick.game.engine.drawables.shapes

import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem

class DrawableShapesSystem(
    private val shapesCollector: (IDrawableShape) -> Unit,
    var debug: Boolean = false,
    private val diagnostics: RuntimeDiagnostics? = null
) : GameSystem(DrawableShapesComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("DrawableShapesSystem")

        entities.forEach { entity ->
            try {
                val shapeComponent = entity.getComponent(DrawableShapesComponent::class)!!

                shapeComponent.prodShapeSuppliers.forEach { shapeSupplier ->
                    shapeSupplier()?.let { shapesCollector.invoke(it) }
                }

                if (debug && shapeComponent.debug)
                    shapeComponent.debugShapeSuppliers.forEach { shapeSupplier ->
                        shapeSupplier()?.let { shapesCollector.invoke(it) }
                    }
            } catch (e: Exception) {
                throw Exception("Exception occured while processing drawable shapes for entity: $entity", e)
            }
        }

        diagnostics?.endEntry()
    }
}
