package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.GameObjectPools

fun GameRectangle.getSize(reclaim: Boolean = true) = getSize(GameObjectPools.fetch(Vector2::class, reclaim))

fun GameRectangle.getCenter(reclaim: Boolean = true) = getCenter(GameObjectPools.fetch(Vector2::class, reclaim))

fun GameRectangle.getPosition(reclaim: Boolean = true) = getPosition(GameObjectPools.fetch(Vector2::class, reclaim))

fun GameRectangle.getPositionPoint(position: Position, reclaim: Boolean = true) =
    getPositionPoint(position, GameObjectPools.fetch(Vector2::class, reclaim))
