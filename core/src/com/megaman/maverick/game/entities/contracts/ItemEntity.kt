package com.megaman.maverick.game.entities.contracts

import com.megaman.maverick.game.entities.megaman.Megaman

interface ItemEntity {

  fun contactWithPlayer(megaman: Megaman)
}
