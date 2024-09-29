package com.megaman.maverick.game.entities

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

object MegaGameEntitiesMap {

    private val map = OrderedMap<EntityType, OrderedSet<MegaGameEntity>>()

    fun add(entity: MegaGameEntity) {
        val type = entity.getEntityType()
        if (!map.containsKey(type)) map.put(type, OrderedSet())
        map.get(type).add(entity)
    }

    fun remove(entity: MegaGameEntity) = map.get(entity.getEntityType())?.remove(entity)

    fun getEntitiesOfType(type: EntityType): OrderedSet<MegaGameEntity> = map.putIfAbsentAndGet(type, OrderedSet())

    fun forEachEntry(action: (EntityType, OrderedSet<MegaGameEntity>) -> Unit) =
        map.forEach { action(it.key, it.value) }

    fun forEachEntity(action: (MegaGameEntity) -> Unit) = map.forEach { entry -> entry.value.forEach { action(it) } }
}