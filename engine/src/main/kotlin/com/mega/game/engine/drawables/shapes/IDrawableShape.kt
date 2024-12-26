package com.mega.game.engine.drawables.shapes

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType

interface IDrawableShape {

    var drawingColor: Color
    var drawingShapeType: ShapeType

    fun draw(renderer: ShapeRenderer): IDrawableShape
}
