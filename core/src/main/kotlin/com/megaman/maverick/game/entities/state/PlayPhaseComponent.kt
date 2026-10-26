package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.IGameComponent

class PlayPhaseComponent(var phase: PlayPhase = PlayPhase.PRE_PLAY) : IGameComponent {

    override fun reset() {}
}
