package com.maverick.game.engine.groups

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.common.extensions.putIfAbsentAndGet
import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.common.objects.MutableOrderedSet
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem

class GroupsSystem : GameSystem(GroupsComponent::class) {

    companion object {
        private val NO_ENTITIES: Collection<IGameEntity> = MutableOrderedSet()
    }

    private val groupToEntities = OrderedMap<String, MutableOrderedSet<IGameEntity>>()
    private val entityToGroups = ObjectMap<IGameEntity, Array<String>>()

    fun getEntitiesInGroup(group: String): Collection<IGameEntity> = groupToEntities.get(group) ?: NO_ENTITIES

    override fun onEntityJoined(entity: IGameEntity) {
        val groups = entity.getComponent(GroupsComponent::class)!!.groups
        if (groups.isEmpty) return

        val joined = Array<String>(groups.size)
        groups.forEach { group ->
            groupToEntities.putIfAbsentAndGet(group) { MutableOrderedSet() }.add(entity)
            joined.add(group)
        }
        entityToGroups.put(entity, joined)
    }

    override fun onEntityLeft(entity: IGameEntity) {
        val groups = entityToGroups.remove(entity) ?: return
        groups.forEach { group ->
            val set = groupToEntities.get(group) ?: return@forEach
            set.remove(entity)
            if (set.isEmpty()) groupToEntities.remove(group)
        }
    }

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {}
}
