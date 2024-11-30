package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.ObjectPools

fun GameRectangle.getSize() = getSize(ObjectPools.get(Vector2::class))

fun GameRectangle.getCenter() = getCenter(ObjectPools.get(Vector2::class))

fun GameRectangle.getPosition() = getPosition(ObjectPools.get(Vector2::class))

fun GameRectangle.getPositionPoint(position: Position) = getPositionPoint(position, ObjectPools.get(Vector2::class))
