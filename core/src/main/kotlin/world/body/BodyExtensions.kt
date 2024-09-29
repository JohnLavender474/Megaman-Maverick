package com.megaman.maverick.game.world.body

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.Body
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

fun Body.setEntity(entity: IBodyEntity) {
    fixtures.forEach { (_, fixture) -> fixture.setEntity(entity) }
    putProperty(ConstKeys.ENTITY, entity)
}

fun Body.getEntity(): MegaGameEntity = getProperty(ConstKeys.ENTITY) as MegaGameEntity

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