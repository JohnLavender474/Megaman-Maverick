package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.motion.RotatingLine
import com.megaman.maverick.game.utils.GameObjectPools

fun RotatingLine.getOrigin() = getOrigin(GameObjectPools.fetch(Vector2::class))

fun RotatingLine.getStartPoint() = getStartPoint(GameObjectPools.fetch(Vector2::class))

fun RotatingLine.getEndPoint() = getEndPoint(GameObjectPools.fetch(Vector2::class))
