package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.motion.IMotion
import com.megaman.maverick.game.utils.GameObjectPools

fun IMotion.getMotionValue() = getMotionValue(GameObjectPools.fetch(Vector2::class))
