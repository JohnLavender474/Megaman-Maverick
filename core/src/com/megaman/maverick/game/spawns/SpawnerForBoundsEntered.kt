package com.megaman.maverick.game.spawns

import com.mega.game.engine.GameEngine
import com.mega.game.engine.common.shapes.IGameShape2D

class SpawnerForBoundsEntered(
    engine: GameEngine,
    private val spawnSupplier: () -> Spawn,
    private val thisBounds: () -> IGameShape2D,
    private val otherBounds: () -> IGameShape2D,
    shouldBeCulled: (Float) -> Boolean = { false },
    onCull: () -> Unit = {},
    respawnable: Boolean = true
) : Spawner(engine, shouldBeCulled, onCull, respawnable) {

    companion object {
        const val TAG = "SpawnerForBoundsEntered"
    }

    private var isEntered = false

    override fun test(delta: Float): Boolean {
        if (!super.test(delta)) return false

        val wasEntered = isEntered
        isEntered = thisBounds().overlaps(otherBounds())
        if (!wasEntered && isEntered) spawn = spawnSupplier()

        return spawned
    }

    override fun reset() {
        super.reset()
        isEntered = false
    }

    override fun toString() = TAG
}
