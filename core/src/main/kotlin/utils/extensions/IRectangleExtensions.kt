package com.megaman.maverick.game.utils.extensions

import com.mega.game.engine.common.interfaces.IRectangle
import com.megaman.maverick.game.utils.LoopedSuppliers

class IRectangleExtensions {

    fun IRectangle.getPosition() = getPosition(LoopedSuppliers.getVector2())

    fun IRectangle.getCenter() = getCenter(LoopedSuppliers.getVector2())
}
