package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.interfaces.IRectangle
import com.megaman.maverick.game.utils.GameObjectPools

fun IRectangle.getPosition() = getPosition(GameObjectPools.fetch(Vector2::class))

fun IRectangle.getCenter() = getCenter(GameObjectPools.fetch(Vector2::class))

fun IRectangle.getRandomPositionInBounds() = getRandomPositionInBounds(GameObjectPools.fetch(Vector2::class))

