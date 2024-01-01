package com.megaman.maverick.game.entities.factories

import com.engine.entities.IGameEntity
import com.engine.factories.Factories
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.impl.*

object EntityFactories : Factories<IGameEntity>() {

  fun initialize(game: MegamanMaverickGame) {
    factories.put(EntityType.BLOCK, BlocksFactory(game))
    factories.put(EntityType.PROJECTILE, ProjectilesFactory(game))
    factories.put(EntityType.EXPLOSION, ExplosionsFactory(game))
    factories.put(EntityType.DECORATION, DecorationsFactory(game))
    factories.put(EntityType.ENEMY, EnemiesFactory(game))
    factories.put(EntityType.ITEM, ItemsFactory(game))
    factories.put(EntityType.SPECIAL, SpecialsFactory(game))
    factories.put(EntityType.HAZARD, HazardsFactory(game))
    factories.put(EntityType.SENSOR, SensorsFactory(game))
  }
}
