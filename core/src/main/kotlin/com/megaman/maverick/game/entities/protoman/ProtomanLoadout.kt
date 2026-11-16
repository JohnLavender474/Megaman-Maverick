package com.megaman.maverick.game.entities.protoman

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.megaman.maverick.game.players.CharacterLoadout
import com.megaman.maverick.game.players.PlayerCharacter

val PROTOMAN_LOADOUT: CharacterLoadout<Protoman> = CharacterLoadout(
    character = PlayerCharacter.PROTOMAN,
    looksInSelectOrder = OrderedMap(),
    rewards = ObjectMap()
)
