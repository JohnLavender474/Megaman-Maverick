package com.megaman.maverick.game.spawns

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

    override fun get(): Spawn? = spawn

    override fun shouldTest(delta: Float) = this.shouldTestPred.invoke(delta)

    final override fun test(delta: Float): Boolean {
        if (spawn?.entity?.dead == true) spawn = null
        if (spawned) return false

        spawn = trySpawn(delta)
        return spawned
    }

    /**
     * Called only when this spawner holds no live spawn. Returns a new spawn if the trigger fired in this call, or null.
     */
    protected abstract fun trySpawn(delta: Float): Spawn?

    override fun shouldBeCulled(delta: Float) = shouldBeCulled.invoke(delta)

    override fun onSpawnRejected() {
        spawn = null
    }

    override fun reset() = onCull()
}
