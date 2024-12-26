package com.mega.game.engine.points

import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem


class PointsSystem : GameSystem(PointsComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        entities.forEach { entity ->
            val pointsComponent = entity.getComponent(PointsComponent::class)
            pointsComponent?.pointsListeners?.forEach { e ->
                val key = e.key
                val listener = e.value

                val points = pointsComponent.pointsMap[key]
                if (points != null) listener.invoke(points)
            }
        }
    }
}
