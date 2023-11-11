package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.hazards.LaserBeamer
import com.megaman.maverick.game.entities.hazards.Saw

/** A factory that creates hazards. */
class HazardsFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

  companion object {
    const val SAW = "Saw"
    const val LASER_BEAMER = "LaserBeamer"
  }

  private val pools = ObjectMap<Any, Pool<IGameEntity>>()

  init {
    // saws
    pools.put(SAW, EntityPoolCreator.create(3) { Saw(game) })
    // laser beamers
    pools.put(LASER_BEAMER, EntityPoolCreator.create(3) { LaserBeamer(game) })
  }

  /**
   * Fetches a hazard from the pool.
   *
   * @param key The key of the hazard to fetch.
   * @return A hazard from the pool.
   */
  override fun fetch(key: Any) = pools.get(key)?.fetch()
}
