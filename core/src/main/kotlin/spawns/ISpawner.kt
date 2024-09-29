package com.megaman.maverick.game.spawns

import com.mega.game.engine.common.interfaces.UpdatePredicate
import com.mega.game.engine.cullables.ICullable

/**
 * Acts as the spawner of an entity. If the [test] method returns true, then the [get] method can be
 * called to retrieve the [Spawn]. If the [shouldBeCulled] method returns true, then this [Spawn]
 * should no longer be considered for spawning.
 *
 * @see UpdatePredicate
 * @see ICullable
 * @see Spawn
 */
interface ISpawner : UpdatePredicate, ICullable {

    /**
     * Returns true if this [ISpawner] should be considered again for spawning after the first spawn.
     */
    var respawnable: Boolean

    /**
     * Gets the [Spawn] if the [test] method returns true.
     *
     * @return the [Spawn] if the [test] method returns true, or null otherwise.
     */
    fun get(): Spawn?

    /**
     * Returns true if this [ISpawner] should be culled. By default, returns false always.
     *
     * @return true if the [Spawn] should be culled, otherwise false
     */
    override fun shouldBeCulled(delta: Float) = false

    /** Called when this spawner is culled. Optional. */
    override fun reset() {}
}
