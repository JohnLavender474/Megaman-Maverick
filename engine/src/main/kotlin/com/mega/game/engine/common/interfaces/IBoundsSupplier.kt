package com.mega.game.engine.common.interfaces

import com.mega.game.engine.common.shapes.GameRectangle


interface IBoundsSupplier {


    fun getBounds(): GameRectangle
}