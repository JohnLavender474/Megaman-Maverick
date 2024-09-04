package com.megaman.maverick.game.spawns

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.GameEngine
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener

class SpawnerForEvent(
    engine: GameEngine,
    private val predicate: (Event) -> Boolean,
    private val spawnSupplier: () -> Spawn,
    override val eventKeyMask: ObjectSet<Any> = ObjectSet(),
    shouldBeCulled: (Float) -> Boolean = { false },
    onCull: () -> Unit = {},
    respawnable: Boolean = true
) : Spawner(engine, shouldBeCulled, onCull, respawnable), IEventListener {

    private val events = Array<Event>()

    override fun test(delta: Float): Boolean {
        if (!super.test(delta)) return false

        for (event in events) if (predicate(event)) {
            spawn = spawnSupplier()
            break
        }
        events.clear()

        return spawned
    }

    override fun onEvent(event: Event) {
        if (!spawned) events.add(event)
    }

    override fun toString() = "SpawnerForEvent[eventKeyMask=$eventKeyMask]"
}
