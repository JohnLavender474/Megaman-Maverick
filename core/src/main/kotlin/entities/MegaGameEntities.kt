package com.megaman.maverick.game.entities

import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

object MegaGameEntities {

    private val entities = OrderedSet<MegaGameEntity>()
    private val entityTypeToEntities = OrderedMap<EntityType, OrderedSet<MegaGameEntity>>()
    private val mapObjectIdToEntities = OrderedMap<Int, OrderedSet<MegaGameEntity>>()
    private val entityTagToEntities = OrderedMap<String, OrderedSet<MegaGameEntity>>()

    fun add(entity: MegaGameEntity) {
        entities.add(entity)
        entityTypeToEntities.putIfAbsentAndGet(entity.getEntityType(), OrderedSet()).add(entity)
        mapObjectIdToEntities.putIfAbsentAndGet(entity.mapObjectId, OrderedSet()).add(entity)
        entityTagToEntities.putIfAbsentAndGet(entity.getTag(), OrderedSet()).add(entity)
    }

    fun remove(entity: MegaGameEntity) {
        entities.remove(entity)
        entityTypeToEntities.get(entity.getEntityType())?.remove(entity)
        entityTagToEntities.get(entity.getTag())?.remove(entity)
        if (mapObjectIdToEntities.containsKey(entity.mapObjectId)) {
            val set = mapObjectIdToEntities.get(entity.mapObjectId)
            set.remove(entity)
            if (set.isEmpty) mapObjectIdToEntities.remove(entity.mapObjectId)
        }
    }

    fun getEntitiesOfTag(tag: String): OrderedSet<MegaGameEntity> = entityTagToEntities.get(tag, OrderedSet())

    fun getEntitiesOfType(type: EntityType): OrderedSet<MegaGameEntity> = entityTypeToEntities.get(type, OrderedSet())

    fun hasAnyEntitiesOfMapObjectId(mapObjectId: Int) = !getEntitiesOfMapObjectId(mapObjectId).isEmpty

    fun getEntitiesOfMapObjectId(mapObjectId: Int): OrderedSet<MegaGameEntity> =
        mapObjectIdToEntities.get(mapObjectId, OrderedSet())

    fun forEachEntity(action: (MegaGameEntity) -> Unit) = entities.forEach { action.invoke(it) }
}
