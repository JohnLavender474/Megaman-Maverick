package com.megaman.maverick.game.players

import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.PlayerView
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent

class MegaPlayer(override val slot: PlayerSlot, override val entity: IGameEntity) : PlayerView {

    private val bodyComponent = entity.require(BodyComponent::class)

    val body: Body
        get() = bodyComponent.body
}
