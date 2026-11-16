package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.GameObjectPools

fun Rectangle.toGameRectangle(): GameRectangle = GameObjectPools.fetch(GameRectangle::class).set(this)

fun Rectangle.getPositionPoint(position: Position) = toGameRectangle().getPositionPoint(position)

fun Rectangle.getCenter(): Vector2 = getCenter(GameObjectPools.fetch(Vector2::class))
