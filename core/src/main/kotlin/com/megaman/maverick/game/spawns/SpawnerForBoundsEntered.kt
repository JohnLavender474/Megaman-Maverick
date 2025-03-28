package com.megaman.maverick.game.spawns

import com.mega.game.engine.common.shapes.IGameShape2D

class SpawnerForBoundsEntered(
    var spawnSupplier: () -> Spawn,
    var thisBounds: () -> IGameShape2D,
    var otherBounds: () -> IGameShape2D,
    shouldBeCulled: (Float) -> Boolean = { false },
    onCull: () -> Unit = {},
    respawnable: Boolean = true,
    shouldTestPred: (Float) -> Boolean = { true }
) : Spawner(shouldBeCulled, onCull, respawnable, shouldTestPred) {

    companion object {
        const val TAG = "SpawnerForBoundsEntered"
    }

    private var isEntered = false

    override fun test(delta: Float): Boolean {
        if (!super.test(delta)) return false

        val wasEntered = isEntered
        isEntered = thisBounds().overlaps(otherBounds())
        val shouldSpawn = !wasEntered && isEntered
        if (shouldSpawn) spawn = spawnSupplier()

        return spawned
    }

    override fun reset() {
        super.reset()
        isEntered = false
    }

    override fun toString() = TAG
}
