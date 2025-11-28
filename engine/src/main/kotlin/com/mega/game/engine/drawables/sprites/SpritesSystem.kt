package com.mega.game.engine.drawables.sprites

import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem

class SpritesSystem(private val collector: (GameSprite) -> Unit) :
    GameSystem(SpritesComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        entities.forEach { entity ->
            val component = entity.getComponent(SpritesComponent::class)!!
            if (component.doUpdateAndDraw.invoke(delta)) {
                component.preProcess(delta)
                component.sprites.values().forEach { sprite -> collector.invoke(sprite) }
                component.postProcess(delta)
            }
        }
    }
}
