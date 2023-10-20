package com.megaman.maverick.game.entities.contracts

import com.engine.entities.IGameEntity
import com.megaman.maverick.game.entities.megaman.Megaman

/** An entity that can be picked up by the player. */
interface ItemEntity : IGameEntity {

  /**
   * Called when the player contacts with this entity.
   *
   * @param megaman the player
   */
  fun contactWithPlayer(megaman: Megaman)
}
