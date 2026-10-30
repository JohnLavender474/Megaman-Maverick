package com.maverick.game.engine.camera

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameRectangle

interface IFocusable {

    fun getFocusBounds(): GameRectangle

    fun getFocusPosition(): Vector2
}
