package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.math.Circle
import com.mega.game.engine.common.shapes.toGameCircle
import com.megaman.maverick.game.utils.LoopedSuppliers

fun Circle.toGameCircle() = toGameCircle(LoopedSuppliers.getGameCircle())
