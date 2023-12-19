package com.megaman.maverick.game.entities.factories

import com.engine.common.GameLogger
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity

/** This class is used to create a pool of entities. */
object EntityPoolCreator {

  /**
   * Creates a pool of entities.
   *
   * @param startAmount the initial amount of entities in the pool.
   * @param supplier the supplier of the entities.
   * @return a pool of entities.
   */
  fun <T : IGameEntity> create(startAmount: Int = 10, supplier: () -> T): Pool<T> {
    val pool = Pool(startAmount, supplier)
    pool.onSupplyNew = { e -> e.runnablesOnDestroy.add { pool.pool(e) } }
    return pool
  }
}
