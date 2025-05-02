package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.midPoint
import com.mega.game.engine.common.objects.IntPair
import com.megaman.maverick.game.utils.GameObjectPools

fun Vector2.toIntPair() = GameObjectPools.fetch(IntPair::class).set(x.toInt(), y.toInt())

fun Vector2.pooledCopy(): Vector2 = GameObjectPools.fetch(Vector2::class).set(this)

fun Vector2.midPoint(other: Vector2) = midPoint(other, GameObjectPools.fetch(Vector2::class))
