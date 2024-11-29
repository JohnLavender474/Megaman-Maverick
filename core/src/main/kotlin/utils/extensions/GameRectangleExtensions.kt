package com.megaman.maverick.game.utils.extensions

import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.LoopedSuppliers

fun GameRectangle.getSize() = getSize(LoopedSuppliers.getVector2())

fun GameRectangle.getCenter() = getCenter(LoopedSuppliers.getVector2())

fun GameRectangle.getPosition() = getPosition(LoopedSuppliers.getVector2())

fun GameRectangle.getPositionPoint(position: Position) = getPositionPoint(position, LoopedSuppliers.getVector2())
