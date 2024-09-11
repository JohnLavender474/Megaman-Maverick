package com.megaman.maverick.game.spawns

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectSetOf

val debugFilterByEntityTag = objectSetOf<String>()

open class Spawner(
    protected val shouldBeCulled: (Float) -> Boolean = { false },
    protected val onCull: () -> Unit = {},
    override var respawnable: Boolean = true
) : ISpawner {

    companion object {
        const val TAG = "Spawner"
    }

    val spawned: Boolean
        get() = spawn != null

    protected var spawn: Spawn? = null

    override fun get(): Spawn? = spawn

    override fun test(delta: Float): Boolean {
        if (spawn?.entity?.dead == true) {
            if (debugFilterByEntityTag.contains(spawn!!.entity.getTag())) GameLogger.debug(
                TAG,
                "destroying spawner: ${spawn!!.entity}"
            )
            spawn = null
        }
        return !spawned
    }

    override fun shouldBeCulled(delta: Float) = shouldBeCulled.invoke(delta)

    override fun reset() = onCull()
}
