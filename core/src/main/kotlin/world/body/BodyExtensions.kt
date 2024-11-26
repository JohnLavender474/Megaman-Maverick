package com.megaman.maverick.game.world.body

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
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

fun Body.hasBlockFilters(): Boolean {
    val filters = getBlockFilters()
    return filters != null && !filters.isEmpty
}

fun Body.getBlockFilters(): Array<(dynamic: MegaGameEntity, static: MegaGameEntity) -> Boolean>? =
    getProperty(ConstKeys.BLOCK_FILTERS) as Array<(MegaGameEntity, MegaGameEntity) -> Boolean>?

fun Body.addBlockFilter(filter: (dynamic: MegaGameEntity, static: MegaGameEntity) -> Boolean) {
    if (!hasBlockFilters()) putProperty(
        ConstKeys.BLOCK_FILTERS, Array<(dynamic: MegaGameEntity, static: MegaGameEntity) -> Boolean>()
    )
    getBlockFilters()!!.add(filter)
}

fun Body.clearBlockFilters() = removeProperty(ConstKeys.BLOCK_FILTERS)
