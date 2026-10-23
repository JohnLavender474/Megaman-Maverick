package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.players.IPlayers
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.Players
import com.maverick.game.engine.players.minByOrNull
import com.megaman.maverick.game.entities.contracts.IMegaGameEntity
import com.megaman.maverick.game.world.body.getCenter

class MegaPlayers(capacity: Int = 1) : Players<MegaPlayer>(capacity, ::MegaPlayer) {

    // TODO: always gives slot 0; the targeting policy (a TargetPolicy with a game default and an optional per-entity
    //  TargetingComponent, see "Targeting design" in docs/mega-refactor/items/029-player-contract/NOTES.md) comes
    //  with its first real caller (item 047, or a boss in 029c)
    fun targetFor(@Suppress("UNUSED_PARAMETER") seeker: IMegaGameEntity): MegaPlayer = get(PlayerSlot(0))!!
}

fun IPlayers<MegaPlayer>.nearestTo(point: Vector2) = minByOrNull { it.body.getCenter().dst2(point) }
