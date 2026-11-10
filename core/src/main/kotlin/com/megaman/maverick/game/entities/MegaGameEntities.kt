package com.megaman.maverick.game.entities

import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.common.extensions.putIfAbsentAndGet
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.mapObjectId
import com.megaman.maverick.game.entities.state.tags

object MegaGameEntities {

    private val entities = OrderedSet<MegaGameEntity>()
    // TODO: one shared empty set, so that a lookup with no entities does not allocate a new set on each call. It
    //  guards only against `add`, and the lookups still give out the mutable index sets. Find a more robust design
    //  (see docs/issues/2026-11-10-mega-game-entities-shared-empty-set.md)
    private val noEntities = NoEntities()
    private val idToEntities = IntMap<OrderedSet<MegaGameEntity>>()
    private val entityTagToEntities = OrderedMap<String, OrderedSet<MegaGameEntity>>()
    private val entityTypeToEntities = OrderedMap<EntityType, OrderedSet<MegaGameEntity>>()

    fun add(entity: MegaGameEntity) {
        entities.add(entity)
        entity.mapObjectId?.let { id ->
            val set = idToEntities.get(id) ?: OrderedSet<MegaGameEntity>().also { idToEntities.put(id, it) }
            set.add(entity)
        }
        val tags = entity.tags
        for (i in 0 until tags.size) entityTagToEntities.putIfAbsentAndGet(tags[i]) { OrderedSet() }.add(entity)
        entityTypeToEntities.putIfAbsentAndGet(entity.getType()) { OrderedSet() }.add(entity)
    }

    fun remove(entity: MegaGameEntity) {
        entities.remove(entity)
        val tags = entity.tags
        for (i in 0 until tags.size) entityTagToEntities.get(tags[i])?.remove(entity)
        entityTypeToEntities.get(entity.getType())?.remove(entity)
        val id = entity.mapObjectId ?: return
        val set = idToEntities.get(id) ?: return
        set.remove(entity)
        if (set.isEmpty) idToEntities.remove(id)
    }

    inline fun <reified T: MegaGameEntity> getOfTag(tag: String, out: OrderedSet<T>): OrderedSet<T> {
        val entities = getOfTag(tag)
        entities.forEach { out.add(it as T) }
        return out
    }

    fun getOfTag(tag: String): OrderedSet<MegaGameEntity> = entityTagToEntities.get(tag) ?: noEntities

    fun getOfTags(out: ObjectSet<MegaGameEntity>, tags: Iterable<String>): ObjectSet<MegaGameEntity> {
        tags.forEach { tag ->
            val set = getOfTag(tag)
            out.addAll(set)
        }
        return out
    }

    fun getOfType(type: EntityType): OrderedSet<MegaGameEntity> = entityTypeToEntities.get(type) ?: noEntities

    fun getOfTypes(out: ObjectSet<MegaGameEntity>, types: Iterable<EntityType>): ObjectSet<MegaGameEntity> {
        types.forEach { type ->
            val set = getOfType(type)
            out.addAll(set)
        }
        return out
    }

    fun existsAnyOfId(id: Int) = !getOfId(id).isEmpty

    fun getOfId(id: Int): OrderedSet<MegaGameEntity> = idToEntities.get(id) ?: noEntities

    fun forEach(action: (MegaGameEntity) -> Unit) = entities.forEach { action.invoke(it) }

    private class NoEntities : OrderedSet<MegaGameEntity>() {

        override fun add(key: MegaGameEntity): Boolean = throw UnsupportedOperationException(
            "The set that MegaGameEntities gives for a key with no entities is shared and read-only; " +
                "copy it into a set of your own before adding to it"
        )
    }
}
