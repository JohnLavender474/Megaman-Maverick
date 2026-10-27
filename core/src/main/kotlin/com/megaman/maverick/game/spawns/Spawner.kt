package com.megaman.maverick.game.spawns

import com.megaman.maverick.game.ConstKeys

/**
 * Base class for spawners. [test] returns true only when a new spawn was made in that call, so that [SpawnsManager]
 * can take the spawn with [get]. A subclass implements [trySpawn] to decide when to make a new spawn.
 */
abstract class Spawner(
    protected val shouldBeCulled: (Float) -> Boolean = { false },
    protected val onCull: () -> Unit = {},
    override var respawnable: Boolean = true,
    protected val shouldTestPred: (Float) -> Boolean = { true },
) : ISpawner {

    companion object {
        const val TAG = "Spawner"
    }

    val spawned: Boolean
        get() = spawn != null
    private var spawn: Spawn? = null

    private val releaseOnDestroyQueued: () -> Unit = {
        release()
        onReleased()
    }

    override fun get(): Spawn? = spawn

    override fun shouldTest(delta: Float) = this.shouldTestPred.invoke(delta)

    final override fun test(delta: Float): Boolean {
        if (spawned) return false

        val newSpawn = trySpawn(delta) ?: return false
        spawn = newSpawn
        newSpawn.entity.runnablesOnDestroyQueued.put(ConstKeys.SPAWNER, releaseOnDestroyQueued)
        return true
    }

    /**
     * Called only when this spawner holds no live spawn. Returns a new spawn if the trigger fired in this call, or null.
     */
    protected abstract fun trySpawn(delta: Float): Spawn?

    override fun shouldBeCulled(delta: Float) = shouldBeCulled.invoke(delta)

    /**
     * Called when the entity of the held spawn gets a destroy call, after the spawner has let go of it.
     */
    protected open fun onReleased() {}

    private fun release() {
        val entity = spawn?.entity ?: return
        if (entity.runnablesOnDestroyQueued[ConstKeys.SPAWNER] === releaseOnDestroyQueued)
            entity.runnablesOnDestroyQueued.remove(ConstKeys.SPAWNER)
        spawn = null
    }

    override fun onSpawnRejected() = release()

    override fun reset() {
        release()
        onCull()
    }
}
