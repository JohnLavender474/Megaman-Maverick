package com.megaman.maverick.game.players

import com.maverick.game.engine.common.interfaces.IPropertizable
import com.megaman.maverick.game.entities.megaman.Megaman

val Any?.asPlayer: MegaPlayer?
    get() = (this as? IPropertizable)?.getProperty(PlayerKeys.VIEW)

// TODO: the screens read Megaman-only state (current weapon, ammo, running, sprite look) through this cast; the
//  loadout moves to item 024 and the character's look to item 029g, and then this accessor goes
val MegaPlayer.megaman: Megaman
    get() = entity as Megaman
