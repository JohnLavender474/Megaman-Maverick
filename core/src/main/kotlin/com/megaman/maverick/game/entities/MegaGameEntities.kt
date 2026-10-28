package com.megaman.maverick.game.entities

import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.common.extensions.putIfAbsentAndGet
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.mapObjectId

object MegaGameEntities {

    private val entities = OrderedSet<MegaGameEntity>()
    private val idToEntities = OrderedMap<Int, OrderedSet<MegaGameEntity>>()
    private val entityTagToEntities = OrderedMap<String, OrderedSet<MegaGameEntity>>()
    private val entityTypeToEntities = OrderedMap<EntityType, OrderedSet<MegaGameEntity>>()

    fun add(entity: MegaGameEntity) {
        entities.add(entity)
        entity.mapObjectId?.let { id -> idToEntities.putIfAbsentAndGet(id) { OrderedSet() }.add(entity) }
        entityTagToEntities.putIfAbsentAndGet(entity.getTag()) { OrderedSet() }.add(entity)
        entityTypeToEntities.putIfAbsentAndGet(entity.getType()) { OrderedSet() }.add(entity)
    }

    fun remove(entity: MegaGameEntity) {
        entities.remove(entity)
        entityTagToEntities.get(entity.getTag())?.remove(entity)
        entityTypeToEntities.get(entity.getType())?.remove(entity)
        val id = entity.mapObjectId ?: return
        if (idToEntities.containsKey(id)) {
            val set = idToEntities.get(id)
            set.remove(entity)
            if (set.isEmpty) idToEntities.remove(id)
        }
    }

    inline fun <reified T: MegaGameEntity> getOfTag(tag: String, out: OrderedSet<T>): OrderedSet<T> {
        val entities = getOfTag(tag)
        entities.forEach { out.add(it as T) }
        return out
    }

    fun getOfTag(tag: String): OrderedSet<MegaGameEntity> = entityTagToEntities.get(tag, OrderedSet())

    fun getOfTags(out: ObjectSet<MegaGameEntity>, tags: Iterable<String>): ObjectSet<MegaGameEntity> {
        tags.forEach { tag ->
            val set = getOfTag(tag)
            out.addAll(set)
        }
        return out
    }

    fun getOfType(type: EntityType): OrderedSet<MegaGameEntity> = entityTypeToEntities.get(type, OrderedSet())

    fun getOfTypes(out: ObjectSet<MegaGameEntity>, types: Iterable<EntityType>): ObjectSet<MegaGameEntity> {
        types.forEach { type ->
            val set = getOfType(type)
            out.addAll(set)
        }
        return out
    }

    fun existsAnyOfId(id: Int) = !getOfId(id).isEmpty

    fun getOfId(id: Int): OrderedSet<MegaGameEntity> = idToEntities.get(id, OrderedSet())

    fun forEach(action: (MegaGameEntity) -> Unit) = entities.forEach { action.invoke(it) }
}
