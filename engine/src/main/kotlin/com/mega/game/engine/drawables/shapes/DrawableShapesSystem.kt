package com.mega.game.engine.drawables.shapes

import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem
import java.util.function.Consumer

class DrawableShapesSystem(
    private val shapesCollector: (IDrawableShape) -> Unit,
    var debug: Boolean = false
) : GameSystem(DrawableShapesComponent::class) {

    constructor(
        shapesCollector: Consumer<IDrawableShape>,
        debug: Boolean = false
    ) : this(shapesCollector::accept, debug)

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        entities.forEach { e ->
            val shapeComponent = e.getComponent(DrawableShapesComponent::class)!!

            shapeComponent.prodShapeSuppliers.forEach { shapeSupplier ->
                shapeSupplier()?.let { shapesCollector.invoke(it) }
            }

            if (debug && shapeComponent.debug)
                shapeComponent.debugShapeSuppliers.forEach { shapeSupplier ->
                    shapeSupplier()?.let { shapesCollector.invoke(it) }
                }
        }
    }
}
