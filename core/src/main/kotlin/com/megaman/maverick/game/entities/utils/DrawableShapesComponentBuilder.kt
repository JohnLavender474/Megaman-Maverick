package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.world.body.getBounds

class DrawableShapesComponentBuilder {

    private val debugShapes = Array<() -> IDrawableShape?>()
    private val prodShapes = Array<() -> IDrawableShape?>()
    private var debug = true

    fun setDebug(debug: Boolean) = apply {
        this.debug = debug
    }

    fun prod(body: Body, color: Color = body.drawingColor) = apply {
        body.drawingColor = color
        prodShapes.add { body.getBounds() }
    }

    fun prod(fixture: Fixture, color: Color = fixture.drawingColor) = apply {
        fixture.drawingColor = color
        prodShapes.add { fixture.getShape() }
    }

    fun debug(body: Body, color: Color = body.drawingColor) = apply {
        body.drawingColor = color
        debugShapes.add { body.getBounds() }
    }

    fun debug(fixture: Fixture, color: Color = fixture.drawingColor) = apply {
        fixture.drawingColor = color
        debugShapes.add { fixture.getShape() }
    }

    fun build() = DrawableShapesComponent(
        prodShapeSuppliers = prodShapes,
        debugShapeSuppliers = debugShapes,
        debug = debug
    )
}
