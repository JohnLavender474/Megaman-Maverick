package com.megaman.maverick.game.players

import com.maverick.game.engine.entities.IGameEntity
import com.megaman.maverick.game.entities.megaman.Megaman

val Any?.asPlayer: MegaPlayer?
    get() = (this as? IGameEntity)?.getComponent(PlayerComponent::class)?.player

// TODO: the screens read Megaman-only state (current weapon, ammo, running, sprite look) through this cast; the
//  loadout reads move with item 024, and running and the sprite look with the second playable character (Proto
//  Man item); then this accessor goes
val MegaPlayer.megaman: Megaman
    get() = entity as Megaman

// TODO: the shoot animation is Megaman's; it moves to the loadout with item 024
val MegaPlayer.shooting: Boolean
    get() = (entity as? Megaman)?.shooting == true
