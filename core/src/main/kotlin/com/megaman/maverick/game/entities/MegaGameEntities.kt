package com.megaman.maverick.game.entities

import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.mega.game.engine.common.objects.MultiCollectionIterable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

object MegaGameEntities {

    private val entities = OrderedSet<MegaGameEntity>()
    private val entityTypeToEntities = OrderedMap<EntityType, OrderedSet<MegaGameEntity>>()
    private val mapObjectIdToEntities = OrderedMap<Int, OrderedSet<MegaGameEntity>>()
    private val entityTagToEntities = OrderedMap<String, OrderedSet<MegaGameEntity>>()

    fun add(entity: MegaGameEntity) {
        entities.add(entity)
        entityTypeToEntities.putIfAbsentAndGet(entity.getType()) { OrderedSet() }.add(entity)
        mapObjectIdToEntities.putIfAbsentAndGet(entity.mapObjectId) { OrderedSet() }.add(entity)
        entityTagToEntities.putIfAbsentAndGet(entity.getTag()) { OrderedSet() }.add(entity)
    }

    fun remove(entity: MegaGameEntity) {
        entities.remove(entity)
        entityTypeToEntities.get(entity.getType())?.remove(entity)
        entityTagToEntities.get(entity.getTag())?.remove(entity)
        if (mapObjectIdToEntities.containsKey(entity.mapObjectId)) {
            val set = mapObjectIdToEntities.get(entity.mapObjectId)
            set.remove(entity)
            if (set.isEmpty) mapObjectIdToEntities.remove(entity.mapObjectId)
        }
    }

    fun getOfTag(tag: String): OrderedSet<MegaGameEntity> = entityTagToEntities.get(tag, OrderedSet())

    fun getOfTags(out: ObjectSet<MegaGameEntity>, vararg tags: String): ObjectSet<MegaGameEntity> {
        tags.forEach { tag ->
            val set = getOfTag(tag)
            out.addAll(set)
        }
        return out
    }

    fun getOfType(type: EntityType): OrderedSet<MegaGameEntity> = entityTypeToEntities.get(type, OrderedSet())

    fun getOfTypes(vararg types: EntityType): Iterable<MegaGameEntity> {
        val iterable = MultiCollectionIterable<MegaGameEntity>()

        types.forEach { type ->
            val set = getOfType(type)
            iterable.add(set)
        }

        return iterable
    }

    fun hasAnyOfMapObjectId(mapObjectId: Int) = !getOfMapObjectId(mapObjectId).isEmpty

    fun getOfMapObjectId(mapObjectId: Int): OrderedSet<MegaGameEntity> =
        mapObjectIdToEntities.get(mapObjectId, OrderedSet())

    fun forEach(action: (MegaGameEntity) -> Unit) = entities.forEach { action.invoke(it) }
}
