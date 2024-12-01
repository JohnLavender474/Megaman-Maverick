package com.megaman.maverick.game.world.body

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.IBody
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.GameObjectPools

fun IBody.getBounds(reclaim: Boolean = true) = getBounds(GameObjectPools.fetch(GameRectangle::class, reclaim))

fun IBody.getPosition(reclaim: Boolean = true) = getPosition(GameObjectPools.fetch(Vector2::class, reclaim))

fun IBody.getCenter(reclaim: Boolean = true) = getCenter(GameObjectPools.fetch(Vector2::class, reclaim))

fun IBody.setCenterX(x: Float): IBody {
    val center = getCenter()
    setCenter(x, center.y)
    return this
}

fun IBody.setCenterY(y: Float): IBody {
    val center = getCenter()
    setCenter(center.x, y)
    return this
}

fun IBody.getSize(reclaim: Boolean = true) = getSize(GameObjectPools.fetch(Vector2::class, reclaim))

fun IBody.getPositionPoint(position: Position, reclaim: Boolean = true) =
    getPositionPoint(position, GameObjectPools.fetch(Vector2::class, reclaim))

fun IBody.setEntity(entity: IBodyEntity) {
    forEachFixture { it.setEntity(entity) }
    putProperty(ConstKeys.ENTITY, entity)
}

fun IBody.getEntity(): MegaGameEntity = getProperty(ConstKeys.ENTITY) as MegaGameEntity

fun IBody.getPositionDelta(): Vector2 {
    val prior = getProperty(key = ConstKeys.PRIOR, Vector2::class)!!
    val current = getPosition().cpy()
    return current.sub(prior)
}

fun IBody.hasBlockFilters(): Boolean {
    val filters = getBlockFilters()
    return filters != null && !filters.isEmpty
}

fun IBody.getBlockFilters(): Array<(dynamic: MegaGameEntity, static: MegaGameEntity) -> Boolean>? =
    getProperty(ConstKeys.BLOCK_FILTERS) as Array<(MegaGameEntity, MegaGameEntity) -> Boolean>?

fun IBody.addBlockFilter(filter: (dynamic: MegaGameEntity, static: MegaGameEntity) -> Boolean) {
    if (!hasBlockFilters()) putProperty(
        ConstKeys.BLOCK_FILTERS, Array<(dynamic: MegaGameEntity, static: MegaGameEntity) -> Boolean>()
    )
    getBlockFilters()!!.add(filter)
}

fun IBody.clearBlockFilters() = removeProperty(ConstKeys.BLOCK_FILTERS)
