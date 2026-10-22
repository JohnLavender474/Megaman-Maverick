package com.megaman.maverick.game.spawns

import com.maverick.game.engine.common.interfaces.UpdatePredicate
import com.maverick.game.engine.cullables.ICullable
import java.util.function.Supplier

interface ISpawner : Supplier<Spawn?>, UpdatePredicate, ICullable {

    var respawnable: Boolean

    fun shouldTest(delta: Float): Boolean = true

    override fun shouldBeCulled(delta: Float) = false

    /**
     * Called when the engine rejects the spawn that this spawner supplied. The entity can already be back in its pool,
     * so the spawner must not keep a reference to it.
     */
    fun onSpawnRejected() {}

    override fun reset() {}
}
