package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.motion.IMotion
import com.megaman.maverick.game.utils.ObjectPools

fun IMotion.getMotionValue() = getMotionValue(ObjectPools.get(Vector2::class))
