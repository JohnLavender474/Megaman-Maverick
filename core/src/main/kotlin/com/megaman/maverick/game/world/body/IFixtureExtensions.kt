package com.megaman.maverick.game.world.body

import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.IBody
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.IWater
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.VelocityAlterator

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

fun IFixture.setVelocityAlteration(alteration: (IFixture, Float, ProcessState) -> VelocityAlteration): IFixture {
    properties.put(ConstKeys.VELOCITY_ALTERATION, alteration)
    return this
}

fun IFixture.getVelocityAlteration(alterableBodyFixture: IFixture, delta: Float, processState: ProcessState) =
    (properties.get(ConstKeys.VELOCITY_ALTERATION) as (IFixture, Float, ProcessState) -> VelocityAlteration)
        .invoke(alterableBodyFixture, delta, processState)

fun IFixture.setFilter(filter: (IFixture) -> Boolean): IFixture {
    putProperty(ConstKeys.FILTER, filter)
    return this
}

fun IFixture.hasFilter() = hasProperty(ConstKeys.FILTER)

fun IFixture.getFilter() = getProperty(ConstKeys.FILTER) as (IFixture) -> Boolean

fun IFixture.setRunnable(runnable: () -> Unit): IFixture {
    properties.put(ConstKeys.RUNNABLE, runnable)
    return this
}

fun IFixture.getRunnable() = properties.get(ConstKeys.RUNNABLE) as (() -> Unit)?

fun IFixture.hasConsumer() = properties.containsKey(ConstKeys.CONSUMER)

fun IFixture.setConsumer(consumer: (ProcessState, IFixture) -> Unit): IFixture {
    properties.put(ConstKeys.CONSUMER, consumer)
    return this
}

fun IFixture.getConsumer() = properties.get(ConstKeys.CONSUMER) as ((ProcessState, IFixture) -> Unit)?

fun IFixture.hasForceAlterationForState(processState: ProcessState) =
    hasProperty("${processState.name.lowercase()}_${ConstKeys.FORCE}_${ConstKeys.VELOCITY_ALTERATION}")

fun IFixture.setForceAlterationForState(processState: ProcessState, listener: (VelocityAlteration) -> Unit): IFixture {
    putProperty("${processState.name.lowercase()}_${ConstKeys.FORCE}_${ConstKeys.VELOCITY_ALTERATION}", listener)
    return this
}

fun IFixture.applyForceAlteration(processState: ProcessState, alteration: VelocityAlteration) {
    if (hasForceAlterationForState(processState)) {
        val runnable = getProperty(
            "${processState.name.lowercase()}_${ConstKeys.FORCE}_${ConstKeys.VELOCITY_ALTERATION}"
        ) as (VelocityAlteration) -> Unit
        runnable.invoke(alteration)
    } else VelocityAlterator.alterate(getBody(), alteration)
}

fun IFixture.setHitByWaterReceiver(receiver: (IWater) -> Unit): IFixture {
    putProperty(ConstKeys.HIT_WATER, receiver)
    return this
}

fun IFixture.hasHitByWaterByReceiver() = hasProperty(ConstKeys.HIT_WATER)

fun IFixture.getHitByWater(water: IWater): IFixture {
    (getProperty(ConstKeys.HIT_WATER) as (IWater) -> Unit).invoke(water)
    return this
}

fun IFixture.setHitByExplosionReceiver(receiver: (IBodyEntity) -> Unit): IFixture {
    putProperty(ConstKeys.HIT_BY_EXPLOSION, receiver)
    return this
}

fun IFixture.hasHitByExplosionReceiver() = hasProperty(ConstKeys.HIT_BY_EXPLOSION)

fun IFixture.getHitByExplosion(explosion: IBodyEntity): IFixture {
    (getProperty(ConstKeys.HIT_BY_EXPLOSION) as (IBodyEntity) -> Unit).invoke(explosion)
    return this
}

fun IFixture.hasHitByBodyReceiver() = hasProperty(ConstKeys.HIT_BY_BODY)

fun IFixture.setHitByBodyReceiver(receiver: (IBodyEntity, ProcessState) -> Unit): IFixture {
    putProperty(ConstKeys.HIT_BY_BODY, receiver)
    return this
}

fun IFixture.getHitByBody(body: IBodyEntity, processState: ProcessState): IFixture {
    (getProperty(ConstKeys.HIT_BY_BODY) as (IBodyEntity, ProcessState) -> Unit).invoke(body, processState)
    return this
}

fun IFixture.hasHitBySideReceiver() = hasProperty(ConstKeys.HIT_BY_SIDE)

fun IFixture.setHitBySideReceiver(receiver: (IFixture, ProcessState) -> Unit): IFixture {
    putProperty(ConstKeys.HIT_BY_SIDE, receiver)
    return this
}

fun IFixture.getHitBySide(side: IFixture, state: ProcessState): IFixture {
    (getProperty(ConstKeys.HIT_BY_SIDE) as (IFixture, ProcessState) -> Unit).invoke(side, state)
    return this
}

fun IFixture.setHitByBlockReceiver(state: ProcessState, receiver: (Block, Float) -> Unit): IFixture {
    putProperty("${ConstKeys.HIT_BY_BLOCK}_${state.name}", receiver)
    return this
}

fun IFixture.hasHitByBlockReceiver(state: ProcessState) =
    hasProperty("${ConstKeys.HIT_BY_BLOCK}_${state.name}")

fun IFixture.getHitByBlock(state: ProcessState, block: Block, delta: Float) =
    (getProperty("${ConstKeys.HIT_BY_BLOCK}_${state.name}") as (Block, Float) -> Unit).invoke(block, delta)

fun IFixture.setHitByPlayerReceiver(receiver: (Megaman) -> Unit): IFixture {
    putProperty(ConstKeys.HIT_BY_PLAYER, receiver)
    return this
}

fun IFixture.hasHitByPlayerReceiver() = hasProperty(ConstKeys.HIT_BY_PLAYER)

fun IFixture.getHitByPlayer(player: Megaman) =
    (getProperty(ConstKeys.HIT_BY_PLAYER) as (Megaman) -> Unit).invoke(player)

fun IFixture.setHitByProjectileReceiver(receiver: (IProjectileEntity) -> Unit): IFixture {
    putProperty(ConstKeys.HIT_BY_PROJECTILE, receiver)
    return this
}

fun IFixture.hasHitByProjectileReceiver() = hasProperty(ConstKeys.HIT_BY_PROJECTILE)

fun IFixture.getHitByProjectile(projectile: IProjectileEntity) =
    (getProperty(ConstKeys.HIT_BY_PROJECTILE) as (IProjectileEntity) -> Unit).invoke(projectile)

fun IFixture.setHitByFeetReceiver(state: ProcessState, receiver: (IFixture, Float) -> Unit): IFixture {
    putProperty("${ConstKeys.HIT_BY_FEET}_${state.name}", receiver)
    return this
}

fun IFixture.hasHitByFeetReceiver(state: ProcessState) = hasProperty("${ConstKeys.HIT_BY_FEET}_${state.name}")

fun IFixture.getHitByFeet(state: ProcessState, feet: IFixture, delta: Float) =
    (getProperty("${ConstKeys.HIT_BY_FEET}_${state.name}") as (IFixture, Float) -> Unit).invoke(feet, delta)

fun IFixture.hasHitByDamageableReceiver() = hasProperty(ConstKeys.HIT_BY_DAMAGEABLE)

fun IFixture.setHitByDamageableReceiver(receiver: (IDamageable, ProcessState) -> Unit): IFixture {
    putProperty(ConstKeys.HIT_BY_DAMAGEABLE, receiver)
    return this
}

fun IFixture.getHitByDamageable(damageable: IDamageable, processState: ProcessState) =
    (getProperty(ConstKeys.HIT_BY_DAMAGEABLE) as (IDamageable, ProcessState) -> Unit).invoke(damageable, processState)

fun IFixture.hasHitByLaserReceiver() = hasProperty(ConstKeys.HIT_BY_LASER)

fun IFixture.setHitByLaserReceiver(receiver: (IFixture, ProcessState) -> Unit): IFixture {
    putProperty(ConstKeys.HIT_BY_LASER, receiver)
    return this
}

fun IFixture.getHitByLaser(laser: IFixture, processState: ProcessState) =
    (getProperty(ConstKeys.HIT_BY_LASER) as (IFixture, ProcessState) -> Unit).invoke(laser, processState)

fun IFixture.shouldStickToBlock(processState: ProcessState, blockFixture: IFixture) = when {
    hasProperty(ConstKeys.STICK_TO_BLOCK) ->
        (getProperty(ConstKeys.STICK_TO_BLOCK) as (ProcessState, IFixture) -> Boolean).invoke(
            processState,
            blockFixture
        )
    else -> true
}

fun IFixture.setShouldStickToBlock(predicate: (ProcessState, IFixture) -> Boolean) {
    putProperty(ConstKeys.STICK_TO_BLOCK, predicate)
}

fun IFixture.setShouldStickToBlock(shouldStick: Boolean) {
    setShouldStickToBlock { _, _ -> shouldStick }
}
