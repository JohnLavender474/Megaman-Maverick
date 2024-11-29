package com.megaman.maverick.game.utils.extensions

import com.mega.game.engine.common.interfaces.IRectangle
import com.megaman.maverick.game.utils.LoopedSuppliers

class IRectangleExtensions {

    fun IRectangle.getPosition() = getPosition(LoopedSuppliers.getVector2())

    fun IRectangle.getCenter() = getCenter(LoopedSuppliers.getVector2())

    fun IRectangle.getTopLeftPoint() = getTopLeftPoint(LoopedSuppliers.getVector2())

    fun IRectangle.getPositionPoint(Position.TOP_CENTER) = getTopCenterPoint(LoopedSuppliers.getVector2())

    fun IRectangle.getTopRightPoint() = getTopRightPoint(LoopedSuppliers.getVector2())

    fun IRectangle.getPositionPoint(Position.CENTER_LEFT) = getCenterLeftPoint(LoopedSuppliers.getVector2())

    fun IRectangle.getCenterPoint() = getCenterPoint(LoopedSuppliers.getVector2())

    fun IRectangle.getPositionPoint(Position.CENTER_RIGHT) = getCenterRightPoint(LoopedSuppliers.getVector2())

    fun IRectangle.getBottomLeftPoint() = getBottomLeftPoint(LoopedSuppliers.getVector2())

    fun IRectangle.getBottomCenterPoint() = getBottomCenterPoint(LoopedSuppliers.getVector2())

    fun IRectangle.getBottomRightPoint() = getBottomRightPoint(LoopedSuppliers.getVector2())
}
