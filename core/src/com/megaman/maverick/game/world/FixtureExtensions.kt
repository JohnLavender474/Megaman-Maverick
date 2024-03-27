package com.megaman.maverick.game.world

import com.engine.common.enums.ProcessState
import com.engine.entities.contracts.IBodyEntity
import com.engine.world.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.utils.VelocityAlteration

fun IFixture.setEntity(entity: IBodyEntity): IFixture {
    properties.put(ConstKeys.ENTITY, entity)
    return this
}

fun IFixture.hasFixtureType(fixtureType: Any) = fixtureType == getFixtureType()

fun IFixture.getEntity() = properties.get(ConstKeys.ENTITY) as IBodyEntity

fun IFixture.depleteHealth(): Boolean {
    val entity = getEntity()
    if (entity !is IHealthEntity) return false

    entity.getHealthPoints().setToMin()
    return true
}

fun IFixture.setVelocityAlteration(alteration: (IFixture, Float) -> VelocityAlteration): IFixture {
    properties.put(ConstKeys.VELOCITY_ALTERATION, alteration)
    return this
}

fun IFixture.getVelocityAlteration(alterableBodyFixture: IFixture, delta: Float) =
    (properties.get(ConstKeys.VELOCITY_ALTERATION) as (IFixture, Float) -> VelocityAlteration).invoke(
            alterableBodyFixture,
            delta
        )

fun IFixture.setRunnable(runnable: () -> Unit): IFixture {
    properties.put(ConstKeys.RUNNABLE, runnable)
    return this
}

fun IFixture.getRunnable() = properties.get(ConstKeys.RUNNABLE) as (() -> Unit)?

fun IFixture.setConsumer(consumer: (ProcessState, IFixture) -> Unit): IFixture {
    properties.put(ConstKeys.CONSUMER, consumer)
    return this
}

fun IFixture.getConsumer() = properties.get(ConstKeys.CONSUMER) as ((ProcessState, IFixture) -> Unit)?
