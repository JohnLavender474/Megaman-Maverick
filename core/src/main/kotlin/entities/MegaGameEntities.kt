package com.megaman.maverick.game.entities

import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

object MegaGameEntitiesMap {

    private val entities = OrderedSet<MegaGameEntity>()
    private val entityTypeToEntities = OrderedMap<EntityType, OrderedSet<MegaGameEntity>>()
    private val mapObjectIdToEntities = OrderedMap<Int, OrderedSet<MegaGameEntity>>()

    fun add(entity: MegaGameEntity) {
        entities.add(entity)
        entityTypeToEntities.putIfAbsentAndGet(entity.getEntityType(), OrderedSet()).add(entity)
        mapObjectIdToEntities.putIfAbsentAndGet(entity.mapObjectId, OrderedSet()).add(entity)
    }

    fun remove(entity: MegaGameEntity) {
        entities.remove(entity)
        entityTypeToEntities.get(entity.getEntityType())?.remove(entity)
        if (mapObjectIdToEntities.containsKey(entity.mapObjectId)) {
            val set = mapObjectIdToEntities.get(entity.mapObjectId)
            set.remove(entity)
            if (set.isEmpty) mapObjectIdToEntities.remove(entity.mapObjectId)
        }
    }

    fun getEntitiesOfType(type: EntityType): OrderedSet<MegaGameEntity> =
        entityTypeToEntities.putIfAbsentAndGet(type, OrderedSet())

    fun getEntitiesOfMapObjectId(mapObjectId: Int) =
        mapObjectIdToEntities.putIfAbsentAndGet(mapObjectId, OrderedSet())

    fun forEachEntity(action: (MegaGameEntity) -> Unit) = entities.forEach { action.invoke(it) }
}
