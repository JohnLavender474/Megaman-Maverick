package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.components.IGameComponent

class RunComponent : IGameComponent {

    var running = false
    var runTime = 0f

    // TODO: a press in a frame when the ControllerSystem does not process Mega (a pause, or Mega not spawned) does not
    //  set this value, so with both buttons held the older press wins until the next press (see
    //  docs/issues/2026-11-01-left-right-both-held.md)
    var lastPressedSide = Facing.RIGHT

    override fun reset() {
        running = false
        runTime = 0f
        lastPressedSide = Facing.RIGHT
    }
}
