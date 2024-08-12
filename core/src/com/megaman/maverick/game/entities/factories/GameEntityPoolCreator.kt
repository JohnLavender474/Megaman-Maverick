package com.megaman.maverick.game.entities.factories

import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity

object GameEntityPoolCreator {

    private const val DEFAULT_START_AMOUNT = 0

    fun create(supplier: () -> IGameEntity) = create(supplier, DEFAULT_START_AMOUNT)

    fun create(supplier: () -> IGameEntity, startAmount: Int): Pool<IGameEntity> {
        val pool = Pool(startAmount, supplier)
        pool.onSupplyNew = { e -> e.runnablesOnDestroy.add { pool.pool(e) } }
        return pool
    }
}
