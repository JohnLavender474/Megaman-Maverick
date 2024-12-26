package com.mega.game.engine.updatables

import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem

class UpdatablesSystem : GameSystem(UpdatablesComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        entities.forEach { entity ->
            val component = entity.getComponent(UpdatablesComponent::class)
            component?.updatables?.values()?.forEach { it.update(delta) }
        }
    }
}
