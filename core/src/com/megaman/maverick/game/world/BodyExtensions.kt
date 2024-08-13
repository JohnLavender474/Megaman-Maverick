package com.megaman.maverick.game.world

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.engine.entities.contracts.IBodyEntity
import com.engine.world.Body
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.megaman.Megaman

fun Body.setEntity(entity: IBodyEntity) {
    fixtures.forEach { (_, fixture) -> fixture.setEntity(entity) }
    putProperty(ConstKeys.ENTITY, entity)
}

fun Body.getEntity(): IBodyEntity = getProperty(ConstKeys.ENTITY) as IBodyEntity

fun Body.getPositionDelta(): Vector2 {
    val prior = getProperty(ConstKeys.PRIOR, Vector2::class)!!
    val current = getPosition().cpy()
    return current.sub(prior)
}

fun Body.getBlockFilters(): ObjectSet<String> {
    var filters = getProperty(ConstKeys.BLOCK_FILTERS) as ObjectSet<String>?
    if (filters == null) {
        filters = ObjectSet<String>()
        putProperty(ConstKeys.BLOCK_FILTERS, filters)
    }
    return filters
}

fun Body.addBlockFilter(key: String) = getBlockFilters().add(key)

fun Body.hasBlockFilter(key: String) = getBlockFilters().contains(key)

fun Body.clearBlockFilters() = putProperty(ConstKeys.BLOCK_FILTERS, ObjectSet<String>())

fun Body.setHitByPlayerReceiver(receiver: (Megaman) -> Unit) {
    putProperty(ConstKeys.HIT_BY_PLAYER, receiver)
}

fun Body.hasHitByPlayerReceiver() = getProperty(ConstKeys.HIT_BY_PLAYER) != null

fun Body.getHitByPlayer(player: Megaman) =
    (getProperty(ConstKeys.HIT_BY_PLAYER) as (Megaman) -> Unit).invoke(player)

fun Body.setHitByProjectileReceiver(receiver: (AbstractProjectile) -> Unit) {
    putProperty(ConstKeys.HIT_BY_PROJECTILE, receiver)
}

fun Body.hasHitByProjectileReceiver() = getProperty(ConstKeys.HIT_BY_PROJECTILE) != null

fun Body.getHitByProjectile(projectile: AbstractProjectile) =
    (getProperty(ConstKeys.HIT_BY_PROJECTILE) as (AbstractProjectile) -> Unit).invoke(projectile)