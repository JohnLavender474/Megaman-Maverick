package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Circle
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.toGameCircle
import com.megaman.maverick.game.utils.GameObjectPools

fun Circle.toGameCircle() = toGameCircle(GameObjectPools.fetch(GameCircle::class))
