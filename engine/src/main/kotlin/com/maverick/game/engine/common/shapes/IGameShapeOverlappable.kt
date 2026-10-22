package com.maverick.game.engine.common.shapes

interface IGameShapeOverlappable {

    fun overlaps(shape: IGameShape2D): Boolean
}
