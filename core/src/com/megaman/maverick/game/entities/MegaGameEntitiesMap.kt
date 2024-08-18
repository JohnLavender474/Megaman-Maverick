package com.megaman.maverick.game.entities

import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet

object MegaGameEntitiesMap {

    private val map = OrderedMap<EntityType, OrderedSet<MegaGameEntity>>()

    fun add(type: EntityType, entity: MegaGameEntity) {
        if (!map.containsKey(type)) map.put(type, OrderedSet())
        map.get(type).add(entity)
    }

    fun remove(type: EntityType, entity: MegaGameEntity) = map.get(type).remove(entity)

    fun get(type: EntityType): OrderedSet<MegaGameEntity> = map.get(type)
}