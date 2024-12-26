package com.mega.game.engine.drawables.shapes

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.components.IGameComponent

class DrawableShapesComponent(
    var prodShapeSuppliers: Array<() -> IDrawableShape?> = Array(),
    var debugShapeSuppliers: Array<() -> IDrawableShape?> = Array(),
    var debug: Boolean = false
) : IGameComponent {

    constructor(vararg shapeSuppliers: () -> IDrawableShape?) : this(Array(shapeSuppliers))

    constructor(vararg shapes: IDrawableShape?) : this(shapes.map { { it } }.toGdxArray())
}
