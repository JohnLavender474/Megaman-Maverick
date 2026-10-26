package com.megaman.maverick.game.players

import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.PlayerView

class MegaPlayer(
    override val slot: PlayerSlot,
    override val entity: IGameEntity,
    private val inputSource: InputSource
) : PlayerView {

    init {
        entity.getComponent(ControllerComponent::class)?.input = inputSource
        // TODO: unregistering a player does not remove this view or its input source from its entity; players who
        //  drop out are a co-op rule for item 047
        entity.addComponent(PlayerComponent(this))
    }

    val input: InputFrame
        get() = inputSource.read()
}
