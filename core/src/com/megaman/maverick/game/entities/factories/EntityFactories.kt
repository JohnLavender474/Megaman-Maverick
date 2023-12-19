package com.megaman.maverick.game.entities.factories

import com.engine.entities.IGameEntity
import com.engine.factories.Factories
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.impl.*

/** A collection of factories that fetch [IGameEntity]s from a key. */
object EntityFactories : Factories<IGameEntity>() {

  /**
   * Initialize the factories. Should be called when game starts.
   *
   * @param game the game
   */
  fun initialize(game: MegamanMaverickGame) {
    factories.put(EntityType.BLOCK, BlocksFactory(game))
    factories.put(EntityType.PROJECTILE, ProjectilesFactory(game))
    factories.put(EntityType.EXPLOSION, ExplosionsFactory(game))
    factories.put(EntityType.DECORATION, DecorationsFactory(game))
    factories.put(EntityType.ENEMY, EnemiesFactory(game))
  }
}
