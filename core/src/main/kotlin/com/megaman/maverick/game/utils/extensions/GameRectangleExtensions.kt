package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.GameObjectPools

fun GameRectangle.getSize() = getSize(GameObjectPools.fetch(Vector2::class))

fun GameRectangle.getCenter() = getCenter(GameObjectPools.fetch(Vector2::class))

fun GameRectangle.getPosition() = getPosition(GameObjectPools.fetch(Vector2::class))

fun GameRectangle.getPositionPoint(position: Position) =
    getPositionPoint(position, GameObjectPools.fetch(Vector2::class))
