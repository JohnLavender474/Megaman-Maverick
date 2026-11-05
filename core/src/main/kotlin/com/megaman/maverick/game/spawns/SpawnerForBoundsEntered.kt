package com.megaman.maverick.game.spawns

import com.maverick.game.engine.common.shapes.IGameShape2D

class SpawnerForBoundsEntered(
    var spawnSupplier: () -> Spawn,
    var thisBounds: () -> IGameShape2D,
    var otherBounds: () -> IGameShape2D,
    shouldBeCulled: (Float) -> Boolean = { false },
    onCull: () -> Unit = {},
    respawnable: Boolean = true,
    shouldTestPred: (Float) -> Boolean = { true },
    var rearmBounds: () -> IGameShape2D = otherBounds,
    var rearmsOnLevelReset: Boolean = false
) : Spawner(shouldBeCulled, onCull, respawnable, shouldTestPred) {

    companion object {
        const val TAG = "SpawnerForBoundsEntered"
    }

    var armed = true
        private set

    override fun track(delta: Float) {
        if (!armed && !spawned && !thisBounds().overlaps(rearmBounds())) armed = true
    }

    override fun trySpawn(delta: Float): Spawn? {
        if (!armed || !thisBounds().overlaps(otherBounds())) return null
        armed = false
        return spawnSupplier()
    }

    override fun onReleased() {
        armed = !thisBounds().overlaps(rearmBounds())
    }

    override fun rearm() {
        if (rearmsOnLevelReset) armed = true
    }

    override fun reset() {
        super.reset()
        armed = true
    }

    override fun toString() = TAG
}
