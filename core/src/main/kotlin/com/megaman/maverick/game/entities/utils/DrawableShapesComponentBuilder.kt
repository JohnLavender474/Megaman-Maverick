package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape

class DrawableShapesComponentBuilder {

    private val component: DrawableShapesComponent
    // private val prodCollection = Array<() -> IDrawableShape?>()
    private val debugCollection = Array<() -> IDrawableShape?>()

    constructor(debug: Boolean = true) {
        component = DrawableShapesComponent(
            debug = debug,
            // prodShapeSuppliers = prodCollection,
            debugShapeSuppliers = debugCollection,
        )
    }

    fun addDebug(supplier: () -> IDrawableShape?): DrawableShapesComponentBuilder {
        debugCollection.add(supplier)
        return this
    }

    fun build() = component
}
