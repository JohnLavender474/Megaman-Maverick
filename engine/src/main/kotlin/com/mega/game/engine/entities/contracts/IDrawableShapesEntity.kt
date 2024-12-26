package com.mega.game.engine.entities.contracts

import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.entities.IGameEntity

interface IDrawableShapesEntity : IGameEntity {

    val drawableShapesComponent: DrawableShapesComponent
        get() {
            val key = DrawableShapesComponent::class
            return getComponent(key)!!
        }

    var isDebugOnForShapes: Boolean
        get() = this.drawableShapesComponent.debug
        set(debug) {
            this.drawableShapesComponent.debug = debug
        }

    fun addProdShapeSupplier(supplier: () -> IDrawableShape?) {
        this.drawableShapesComponent.prodShapeSuppliers.add(supplier)
    }

    fun clearProdShapeSuppliers() {
        this.drawableShapesComponent.prodShapeSuppliers.clear()
    }

    fun addDebugShapeSupplier(supplier: () -> IDrawableShape?) {
        this.drawableShapesComponent.debugShapeSuppliers.add(supplier)
    }

    fun clearDebugShapeSuppliers() {
        this.drawableShapesComponent.debugShapeSuppliers.clear()
    }
}
