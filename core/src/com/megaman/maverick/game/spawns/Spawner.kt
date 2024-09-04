package com.megaman.maverick.game.spawns

import com.mega.game.engine.GameEngine
import java.util.function.Predicate

/**
 * A [Spawner] is used to spawn a new entity when one hasn't been spawned yet.
 *
 * @property engine used to check whether the entity contained in the [spawn] field has been spawned or not.
 * @property shouldBeCulled used to check if this spawner object should be removed from the [SpawnsManager].
 * @property onCull called by [SpawnsManager] when this spawner object is removed from [SpawnsManager].
 * @property respawnable whether the entity of this spawner can be spawned more than once.
 */
abstract class Spawner(
    protected val engine: GameEngine,
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

    /**
     * Constructor for a [Spawner].
     *
     * @param shouldBeCulled the predicate to determine if the spawn should be culled
     * @param onCull the action to take when the spawn is culled
     * @param respawnable if the spawner should be considered again for spawning after the first spawn
     */
    constructor(
        engine: GameEngine,
        shouldBeCulled: Predicate<Float> = Predicate { false },
        onCull: Runnable = Runnable {},
        respawnable: Boolean = true
    ) : this(
        engine,
        shouldBeCulled = shouldBeCulled::test,
        onCull = onCull::run,
        respawnable = respawnable
    )

    override fun get(): Spawn? = spawn

    override fun test(delta: Float): Boolean {
        if (spawn?.entity != null && !spawn!!.entity.spawned) spawn = null
        return !spawned
    }

    override fun shouldBeCulled(delta: Float) = shouldBeCulled.invoke(delta)

    override fun reset() = onCull()
}
