package com.megaman.maverick.game.entities.factories

import com.engine.common.objects.Pool
import com.megaman.maverick.game.entities.MegaGameEntity

object GameEntityPoolCreator {

    private const val DEFAULT_START_AMOUNT = 0

    fun create(supplier: () -> MegaGameEntity) = create(supplier, DEFAULT_START_AMOUNT)

    fun create(supplier: () -> MegaGameEntity, startAmount: Int): Pool<MegaGameEntity> {
        val pool = Pool(startAmount, supplier)
        pool.onSupplyNew = { e -> e.runnablesOnDestroy.add { pool.pool(e) } }
        return pool
    }
}
