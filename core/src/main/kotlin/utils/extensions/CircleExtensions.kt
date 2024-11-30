package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Circle
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.toGameCircle
import com.megaman.maverick.game.utils.ObjectPools

fun Circle.toGameCircle() = toGameCircle(ObjectPools.get(GameCircle::class))
