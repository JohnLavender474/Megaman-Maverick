package com.megaman.maverick.game.players

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class PlayerRoster(private val players: MegaPlayers) {

    companion object {
        const val TAG = "PlayerRoster"
    }

    private val slot0 = ObjectMap<PlayerCharacter, MegaGameEntity>()
    private val all = Array<MegaGameEntity>()

    fun add(entity: MegaGameEntity, slot0Character: PlayerCharacter? = null) {
        all.add(entity)
        if (slot0Character != null) slot0.put(slot0Character, entity)
    }

    fun forEach(action: (MegaGameEntity) -> Unit) = all.forEach(action)

    // TODO: only slot 0 can change its character; with more than 1 player, a level with no MEGAMAN throws, and
    //  co-op as another character waits for item 047; a level with more than 1 character needs the select screen of
    //  item 113
    fun putInSlot0(characters: Set<PlayerCharacter>, playerCount: Int) {
        if (characters.size != 1) throw IllegalArgumentException(
            "A level must name 1 character until the character select of item 113 exists: $characters"
        )
        val character = characters.first()
        if (character != PlayerCharacter.MEGAMAN && playerCount > 1) throw IllegalStateException(
            "$character can play only in single player, not with $playerCount players (item 047)"
        )

        val next = slot0[character] ?: throw IllegalStateException("No entity for $character in the roster")
        val slot = PlayerSlot(0)
        val current = players.get(slot)
        if (current === next) return

        if (current != null) {
            if (current.existenceState.liveNow) throw IllegalStateException(
                "Slot 0 cannot change its player while the player is in the engine: $current"
            )
            players.unregister(slot)
        }
        players.register(slot, next)
        GameLogger.debug(TAG, "putInSlot0(): slot 0 is now $character")
    }
}
