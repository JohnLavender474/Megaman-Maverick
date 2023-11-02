package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.decorations.SmokePuff
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

/** A factory that creates decorations. */
class DecorationsFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

  companion object {
    const val TAG = "DecorationsFactory"

    const val SPLASH = "Splash"
    const val SMOKE_PUFF = "SmokePuff"
  }

  private val pools = ObjectMap<Any, Pool<IGameEntity>>()

  init {
    // splash
    pools.put(SPLASH, EntityPoolCreator.create(5) { Splash(game) })
    // smoke puff
    pools.put(SMOKE_PUFF, EntityPoolCreator.create(5) { SmokePuff(game) })
  }

  /**
   * Fetches a decoration from the pool.
   *
   * @param key The key of the decoration to fetch.
   * @return A decoration from the pool.
   */
  override fun fetch(key: Any): IGameEntity? {
    TODO("Not yet implemented")
  }
}
