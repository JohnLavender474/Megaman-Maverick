package com.megaman.maverick.game.players

import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.Players
import com.megaman.maverick.game.entities.contracts.IMegaGameEntity
import com.megaman.maverick.game.input.InputSource
import com.megaman.maverick.game.input.PlayerInputs

class MegaPlayers(capacity: Int = 1, val inputs: PlayerInputs = PlayerInputs(capacity)) :
    Players<MegaPlayer>(capacity, { slot, entity -> MegaPlayer(slot, entity, InputSource { inputs[slot] }) }) {

    // TODO: always gives slot 0. Targeting should not be a service-wide function, and targetPlayer should not exist on
    //  every entity: not every entity needs a target player (blocks do not), and each entity that does has its own
    //  targeting behavior. It belongs to the entities that need it, as a component or policy that each one owns (see
    //  "Targeting design" in docs/mega-refactor/items/029-player-contract/NOTES.md); item 047, or the first entity
    //  with a rule of its own
    fun targetFor(@Suppress("UNUSED_PARAMETER") seeker: IMegaGameEntity): MegaPlayer = get(PlayerSlot(0))!!
}
