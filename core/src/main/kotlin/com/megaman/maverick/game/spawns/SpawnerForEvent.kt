package com.megaman.maverick.game.spawns

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener

class SpawnerForEvent(
    private val predicate: (Event) -> Boolean,
    private val spawnSupplier: () -> Spawn,
    override val eventKeyMask: ObjectSet<Any> = ObjectSet(),
    shouldBeCulled: (Float) -> Boolean = { false },
    onCull: () -> Unit = {},
    respawnable: Boolean = true,
    shouldTestPred: (Float) -> Boolean = { true }
) : Spawner(shouldBeCulled, onCull, respawnable, shouldTestPred), IEventListener {

    private val events = Array<Event>()

    override fun trySpawn(delta: Float): Spawn? {
        val spawn = if (events.any { predicate(it) }) spawnSupplier() else null
        events.clear()
        return spawn
    }

    override fun onEvent(event: Event) {
        if (!spawned) events.add(event)
    }

    override fun toString() = "SpawnerForEvent[eventKeyMask=$eventKeyMask]"
}
