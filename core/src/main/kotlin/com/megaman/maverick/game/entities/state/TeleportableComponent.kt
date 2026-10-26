package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.IGameComponent
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity

class TeleportableComponent(var teleporter: ITeleporterEntity? = null) : IGameComponent {

    override fun reset() {
        teleporter = null
    }
}
