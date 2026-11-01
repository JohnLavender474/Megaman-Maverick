package com.megaman.maverick.game.entities.state

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.camera.IFocusable
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.components.IGameComponent

class FocusComponent(
    private val bounds: () -> GameRectangle,
    private val position: () -> Vector2
) : IGameComponent, IFocusable {

    override fun getFocusBounds() = bounds()

    override fun getFocusPosition() = position()
}
