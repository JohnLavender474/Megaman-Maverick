package com.megaman.maverick.game.utils.extensions

import com.mega.game.engine.common.shapes.GamePolygon
import com.megaman.maverick.game.utils.LoopedSuppliers

fun GamePolygon.getBoundingRectangle() = getBoundingRectangle(LoopedSuppliers.getGameRectangle())
