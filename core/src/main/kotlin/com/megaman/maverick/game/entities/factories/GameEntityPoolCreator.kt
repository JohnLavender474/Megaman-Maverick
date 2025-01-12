package com.megaman.maverick.game.entities.factories

import com.mega.game.engine.common.objects.Pool
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

object GameEntityPoolCreator {

    private const val DEFAULT_START_AMOUNT = 0

    fun create(supplier: () -> MegaGameEntity) = create(supplier, DEFAULT_START_AMOUNT)

    fun create(supplier: () -> MegaGameEntity, startAmount: Int): Pool<MegaGameEntity> {
        val pool = Pool<MegaGameEntity>(supplier = supplier, startAmount = startAmount)
        pool.onSupplyNew = { e -> e.runnablesOnDestroy.put(ConstKeys.POOL) { pool.free(e) } }
        return pool
    }
}
