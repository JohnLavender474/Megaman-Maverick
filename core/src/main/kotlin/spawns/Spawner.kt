package com.megaman.maverick.game.spawns

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
        if (spawn?.entity?.dead == true) spawn = null
        return !spawned
    }

    override fun shouldBeCulled(delta: Float) = shouldBeCulled.invoke(delta)

    override fun reset() = onCull()
}
