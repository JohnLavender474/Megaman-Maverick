package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Rectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.megaman.maverick.game.utils.LoopedSuppliers

fun IGameShape2D.getBoundingRectangle() = getBoundingRectangle(LoopedSuppliers.getGameRectangle())

fun IGameShape2D.toGdxRectangle(): Rectangle {
    val out = LoopedSuppliers.getGdxRectangle()
    val bounds = getBoundingRectangle()
    out.set(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())
    return out
}

fun IGameShape2D.getCenter() = getBoundingRectangle().getCenter()
