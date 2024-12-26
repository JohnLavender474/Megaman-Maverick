package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem

class MotionSystem : GameSystem(MotionComponent::class) {

    private val out = Vector2()

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        entities.forEach {
            it.getComponent(MotionComponent::class)?.let { motionComponent ->
                motionComponent.definitions.values().forEach { definition ->
                    if (definition.doUpdate()) {
                        val motion = definition.motion
                        motion.update(delta)

                        val value = motion.getMotionValue(out)
                        if (value != null) definition.function(value, delta)
                    }
                }
            }
        }
    }
}
