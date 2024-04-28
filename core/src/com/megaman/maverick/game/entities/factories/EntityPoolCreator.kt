package com.megaman.maverick.game.entities.factories

import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity

object EntityPoolCreator {

    private const val START_AMOUNT = 0

    fun <T : IGameEntity> create(supplier: () -> T): Pool<T> {
        val pool = Pool(START_AMOUNT, supplier)
        pool.onSupplyNew = { e -> e.runnablesOnDestroy.add { pool.pool(e) } }
        return pool
    }
}
