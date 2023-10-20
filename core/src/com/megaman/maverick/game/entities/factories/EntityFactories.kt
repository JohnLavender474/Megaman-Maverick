package com.megaman.maverick.game.entities.factories

import com.engine.IGame2D
import com.engine.entities.IGameEntity
import com.engine.factories.Factories
import com.megaman.maverick.game.entities.EntityType

/** A collection of factories that fetch [IGameEntity]s from a key. */
object EntityFactories : Factories<IGameEntity>() {

  /**
   * Initialize the factories. Should be called when game starts.
   *
   * @param game the game
   */
  fun initialize(game: IGame2D) {
    factories.put(EntityType.BLOCK, BlockFactory(game))
  }
}
