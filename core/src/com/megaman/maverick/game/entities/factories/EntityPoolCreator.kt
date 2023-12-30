package com.megaman.maverick.game.entities.factories

import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity

object EntityPoolCreator {

  fun <T : IGameEntity> create(startAmount: Int = 10, supplier: () -> T): Pool<T> {
    val pool = Pool(startAmount, supplier)
    pool.onSupplyNew = { e -> e.runnablesOnDestroy.add { pool.pool(e) } }
    return pool
  }
}
