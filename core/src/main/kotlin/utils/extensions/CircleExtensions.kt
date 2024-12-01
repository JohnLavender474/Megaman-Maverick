package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Circle
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.toGameCircle
import com.megaman.maverick.game.utils.GameObjectPools

fun Circle.toGameCircle(reclaim: Boolean = true) = toGameCircle(GameObjectPools.fetch(GameCircle::class, reclaim))
