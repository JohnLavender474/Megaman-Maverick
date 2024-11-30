package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.ObjectPools

fun Rectangle.toGameRectangle(): GameRectangle {
    val rect = ObjectPools.get(GameRectangle::class)
    return rect.set(this)
}

fun Rectangle.getPositionPoint(position: Position) = toGameRectangle().getPositionPoint(position)

fun Rectangle.getCenter(): Vector2 = getCenter(ObjectPools.get(Vector2::class))
