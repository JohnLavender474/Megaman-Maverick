package com.megaman.maverick.game.entities.factories

import com.engine.IGame2D
import com.engine.entities.IGameEntity
import com.engine.factories.Factories
import com.megaman.maverick.game.entities.EntityType

object EntityFactories : Factories<IGameEntity>() {

  fun initialize(game: IGame2D) {
    factories.put(EntityType.BLOCK, BlockFactory(game))
  }
}
