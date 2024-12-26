package com.mega.game.engine.cullables

import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem


interface GameEntityCuller {


    fun cull(entity: IGameEntity)
}


class CullablesSystem(private val culler: GameEntityCuller) : GameSystem(CullablesComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return
        entities.forEach { entity ->
            val cullables = entity.getComponent(CullablesComponent::class)?.cullables?.values()
            for (cullable in cullables ?: return) if (cullable.shouldBeCulled(delta)) {
                culler.cull(entity)
                break
            }
        }
    }
}
