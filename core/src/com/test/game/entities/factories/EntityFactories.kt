package com.test.game.entities.factories

import com.engine.IGame2D
import com.engine.entities.IGameEntity
import com.engine.factories.Factories
import com.test.game.entities.EntityType

class EntityFactories(game: IGame2D) : Factories<IGameEntity>() {

  init {
    factories.put(EntityType.BLOCK, BlockFactory(game))
  }
}
