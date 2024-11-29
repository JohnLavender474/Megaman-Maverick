package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.LoopedSuppliers

fun Rectangle.toGameRectangle(): GameRectangle {
    val rect = LoopedSuppliers.getGameRectangle()
    return rect.set(this)
}

fun Rectangle.getPositionPoint(position: Position) = toGameRectangle().getPositionPoint(position)

fun Rectangle.getCenter(): Vector2 = getCenter(LoopedSuppliers.getVector2())
