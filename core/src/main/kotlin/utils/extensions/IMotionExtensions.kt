package com.megaman.maverick.game.utils.extensions

import com.mega.game.engine.motion.IMotion
import com.megaman.maverick.game.utils.LoopedSuppliers

fun IMotion.getMotionValue() = getMotionValue(LoopedSuppliers.getVector2())
