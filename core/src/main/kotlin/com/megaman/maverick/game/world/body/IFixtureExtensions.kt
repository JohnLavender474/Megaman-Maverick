package com.megaman.maverick.game.world.body

import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

fun IFixture.getBody(): IBody = (getEntity() as IBodyEntity).body

fun IFixture.setEntity(entity: IBodyEntity): IFixture {
    properties.put(ConstKeys.ENTITY, entity)
    return this
}

fun IFixture.getEntity() = properties.get(ConstKeys.ENTITY) as MegaGameEntity

fun IFixture.depleteHealth(): Boolean {
    val entity = getEntity()
    if (entity !is IHealthEntity) return false

    entity.getHealthPoints().setToMin()
    return true
}
