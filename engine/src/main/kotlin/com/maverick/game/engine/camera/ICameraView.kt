package com.maverick.game.engine.camera

import com.badlogic.gdx.math.collision.BoundingBox
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.IRectangle
import com.maverick.game.engine.common.shapes.GameRectangle

interface ICameraView {

    val direction: Direction

    val rotating: Boolean

    fun getRotatedBounds(out: GameRectangle): GameRectangle

    fun toGameRectangle(out: GameRectangle): GameRectangle

    fun overlaps(bounds: IRectangle, out: BoundingBox): Boolean
}
