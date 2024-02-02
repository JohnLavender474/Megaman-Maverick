package com.megaman.maverick.game.world

import com.engine.entities.contracts.IBodyEntity
import com.engine.world.Body
import com.megaman.maverick.game.ConstKeys

fun Body.setEntity(entity: IBodyEntity) {
    fixtures.forEach { (_, fixture) -> fixture.setEntity(entity) }
    putProperty(ConstKeys.ENTITY, entity)
}

fun Body.getEntity(): IBodyEntity = getProperty(ConstKeys.ENTITY) as IBodyEntity