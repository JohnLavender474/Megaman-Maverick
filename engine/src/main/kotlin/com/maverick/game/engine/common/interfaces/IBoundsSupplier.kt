package com.maverick.game.engine.common.interfaces

import com.maverick.game.engine.common.shapes.GameRectangle

interface IBoundsSupplier {

    fun getBounds(): GameRectangle
}
