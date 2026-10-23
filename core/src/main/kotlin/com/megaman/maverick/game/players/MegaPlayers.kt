package com.megaman.maverick.game.players

import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.Players
import com.megaman.maverick.game.entities.contracts.IMegaGameEntity

class MegaPlayers(capacity: Int = 1) : Players<MegaPlayer>(capacity, ::MegaPlayer) {

    // TODO: always gives slot 0; the targeting policy (a TargetPolicy with a game default and an optional per-entity
    //  TargetingComponent, see "Targeting design" in docs/mega-refactor/items/029-player-contract/NOTES.md) comes
    //  with its first real caller (item 047, or a boss in 029c)
    fun targetFor(@Suppress("UNUSED_PARAMETER") seeker: IMegaGameEntity): MegaPlayer = get(PlayerSlot(0))!!
}
