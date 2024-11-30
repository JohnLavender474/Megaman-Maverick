package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Rectangle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.megaman.maverick.game.utils.GameObjectPools

fun IGameShape2D.getBoundingRectangle() = getBoundingRectangle(GameObjectPools.fetch(GameRectangle::class))

fun IGameShape2D.toGdxRectangle(): Rectangle {
    val out = GameObjectPools.fetch(Rectangle::class)
    val bounds = getBoundingRectangle()
    out.set(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())
    return out
}

fun IGameShape2D.getCenter() = getBoundingRectangle().getCenter()
