package com.megaman.maverick.game.entities.megamanx

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.megaman.maverick.game.players.CharacterLoadout
import com.megaman.maverick.game.players.PlayerCharacter

val MEGAMAN_X_LOADOUT: CharacterLoadout<MegamanX> = CharacterLoadout(
    character = PlayerCharacter.X,
    looksInSelectOrder = OrderedMap(),
    rewards = ObjectMap()
)
