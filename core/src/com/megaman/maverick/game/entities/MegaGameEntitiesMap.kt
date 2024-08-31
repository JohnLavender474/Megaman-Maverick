package com.megaman.maverick.game.entities

import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.engine.common.extensions.putIfAbsentAndGet
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

object MegaGameEntitiesMap {

    private val map = OrderedMap<EntityType, OrderedSet<MegaGameEntity>>()

    fun add(type: EntityType, entity: MegaGameEntity) {
        if (!map.containsKey(type)) map.put(type, OrderedSet())
        map.get(type).add(entity)
    }

    fun remove(type: EntityType, entity: MegaGameEntity) = map.get(type)?.remove(entity)

    fun get(type: EntityType): OrderedSet<MegaGameEntity> = map.putIfAbsentAndGet(type, OrderedSet())

    fun forEachEntry(action: (EntityType, OrderedSet<MegaGameEntity>) -> Unit) =
        map.forEach { action(it.key, it.value) }

    fun forEachEntity(action: (MegaGameEntity) -> Unit) = map.forEach { entry -> entry.value.forEach { action(it) } }
}