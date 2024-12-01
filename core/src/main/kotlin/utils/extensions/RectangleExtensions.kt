package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.GameObjectPools

fun Rectangle.toGameRectangle(reclaim: Boolean = true): GameRectangle {
    val rect = GameObjectPools.fetch(GameRectangle::class, reclaim)
    return rect.set(this)
}

fun Rectangle.getPositionPoint(position: Position, reclaim: Boolean = true) =
    toGameRectangle(reclaim).getPositionPoint(position)

fun Rectangle.getCenter(reclaim: Boolean = true): Vector2 = getCenter(GameObjectPools.fetch(Vector2::class, reclaim))
