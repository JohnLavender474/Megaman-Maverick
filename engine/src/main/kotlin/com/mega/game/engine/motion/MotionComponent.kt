package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.components.IGameComponent

class MotionComponent : IGameComponent {

    data class MotionDefinition(
        val motion: IMotion,
        val function: (Vector2, Float) -> Unit,
        val doUpdate: () -> Boolean = { true },
        var onReset: (() -> Unit)? = null
    ) : Resettable {

        override fun reset() {
            motion.reset()
            onReset?.invoke()
        }
    }

    val definitions = OrderedMap<Any, MotionDefinition>()

    fun put(key: Any, definition: MotionDefinition): MotionDefinition? = definitions.put(key, definition)

    override fun reset() = definitions.values().forEach { it.reset() }
}
