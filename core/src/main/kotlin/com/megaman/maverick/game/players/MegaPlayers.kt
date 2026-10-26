package com.megaman.maverick.game.players

import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.damage.IDamager
import com.maverick.game.engine.entities.hasBody
import com.maverick.game.engine.events.Signal
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.Players
import com.megaman.maverick.game.entities.contracts.IMegaGameEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.input.PlayerInputs

class MegaPlayers(capacity: Int = 1, val inputs: PlayerInputs = PlayerInputs(capacity)) :
    Players<MegaGameEntity>(capacity) {

    override fun register(slot: PlayerSlot, entity: MegaGameEntity): MegaGameEntity {
        super.register(slot, entity)
        // TODO: unregistering a player does not unbind its input source; players who drop out are a co-op rule for
        //  item 047
        entity.getComponent(ControllerComponent::class)?.input = InputSource { inputs[slot] }
        return entity
    }

    // TODO: always gives slot 0. Targeting should not be a service-wide function, and targetPlayer should not exist on
    //  every entity: not every entity needs a target player (blocks do not), and each entity that does has its own
    //  targeting behavior. It belongs to the entities that need it, as a component or policy that each one owns (see
    //  "Targeting design" in docs/mega-refactor/items/029-player-contract/NOTES.md); item 047, or the first entity
    //  with a rule of its own
    fun targetFor(@Suppress("UNUSED_PARAMETER") seeker: IMegaGameEntity): MegaGameEntity {
        for (i in 0 until capacity) get(PlayerSlot(i))?.let { if (it.hasBody) return it }
        return get(PlayerSlot(0))!!
    }

    val playerDamaged = Signal<PlayerDamaged>()
    private val playerDamagedPayload = PlayerDamaged()

    fun onPlayerDamaged(player: MegaGameEntity, damager: IDamager) {
        playerDamagedPayload.player = player
        playerDamagedPayload.damager = damager
        playerDamaged.emit(playerDamagedPayload)
    }
}
