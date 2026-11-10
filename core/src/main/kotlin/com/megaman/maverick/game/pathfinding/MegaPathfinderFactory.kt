package com.megaman.maverick.game.pathfinding

import com.maverick.game.engine.world.pathfinding.BodyCellsSnapshot
import com.maverick.game.engine.world.IWorldBodies
import com.maverick.game.engine.pathfinding.PathfinderResult
import java.util.concurrent.atomic.AtomicInteger
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.Gdx
import com.maverick.game.engine.pathfinding.IPathfinder
import com.maverick.game.engine.pathfinding.IPathfinderFactory
import com.maverick.game.engine.pathfinding.PathfinderParams
import com.maverick.game.engine.pathfinding.heuristics.EuclideanHeuristic
import com.maverick.game.engine.pathfinding.heuristics.IHeuristic
import com.maverick.game.engine.world.pathfinding.WorldPathfinder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.utils.DynamicBodyHeuristic
import com.megaman.maverick.game.levels.LevelState

class MegaPathfinderFactory(
    private val levelState: LevelState,
    private val worldBodies: IWorldBodies
) : IPathfinderFactory {

    private val defaultHeuristic = EuclideanHeuristic()

    private val snapshots = Array<WorldSnapshot>()
    private var snapshot: WorldSnapshot? = null
    private var snapshotFrameId = -1L

    private fun acquireSnapshot(): WorldSnapshot? {
        if (levelState.worldContainer == null) return null
        val frameId = Gdx.graphics.frameId
        var current = snapshot
        if (current == null || frameId != snapshotFrameId) {
            current = freeSnapshot() ?: WorldSnapshot(BodyCellsSnapshot(ConstVals.PPM)).also { snapshots.add(it) }
            current.cells.fill(worldBodies)
            snapshot = current
            snapshotFrameId = frameId
        }
        current.users.incrementAndGet()
        return current
    }

    private fun freeSnapshot(): WorldSnapshot? {
        for (i in 0 until snapshots.size) if (snapshots[i].users.get() == 0) return snapshots[i]
        return null
    }

    override fun getPathfinder(params: PathfinderParams): IPathfinder {
        val heuristic = params.getOrDefaultProperty(ConstKeys.HEURISTIC, defaultHeuristic, IHeuristic::class)
        val tiledMapResult = levelState.tiledMapLoadResult!!
        val start = params.startCoordinateSupplier()
        val target = params.targetCoordinateSupplier()
        val allowDiagonal = params.allowDiagonal()
        val allowOutOfWorldBounds = params.getOrDefaultProperty(ConstKeys.ALLOW_OUT_OF_BOUNDS, true, Boolean::class)
        val maxIterations = params.getOrDefaultProperty(
            ConstKeys.ITERATIONS,
            ConstVals.DEFAULT_PATHFINDING_MAX_ITERATIONS,
            Int::class
        )
        val maxDistance = params.getOrDefaultProperty(
            ConstKeys.DISTANCE,
            ConstVals.DEFAULT_PATHFINDING_MAX_DISTANCE,
            Int::class
        )
        val returnBestPathOnFailure = params.getOrDefaultProperty(
            ConstKeys.DEFAULT,
            ConstVals.DEFAULT_RETURN_BEST_PATH,
            Boolean::class
        )

        val snapshot = acquireSnapshot()
        val cells = snapshot?.cells
        if (heuristic is DynamicBodyHeuristic) heuristic.cells = cells

        val pathfinder = WorldPathfinder(
            start = start,
            target = target,
            cells = cells,
            worldWidth = tiledMapResult.worldWidth,
            worldHeight = tiledMapResult.worldHeight,
            allowDiagonal = allowDiagonal,
            allowOutOfWorldBounds = allowOutOfWorldBounds,
            filter = params.filter,
            heuristic = heuristic,
            maxIterations = maxIterations,
            maxDistance = maxDistance,
            returnBestPathOnFailure = returnBestPathOnFailure
        )
        return if (snapshot == null) pathfinder else SnapshotPathfinder(pathfinder, snapshot)
    }
}

private class WorldSnapshot(val cells: BodyCellsSnapshot) {

    val users = AtomicInteger()
}

// TODO: a search that is cancelled before it starts, or that the executor rejects, never runs `call`, so its snapshot
//  stays in use and the pool grows by one; the cached thread pool starts each task at once and the timeout is 500 ms,
//  so this does not happen today (docs/issues/2026-11-09-per-frame-allocations-hot-paths.md, short contract)
private class SnapshotPathfinder(private val pathfinder: IPathfinder, private val snapshot: WorldSnapshot) : IPathfinder {

    override fun call(): PathfinderResult = try {
        pathfinder.call()
    } finally {
        snapshot.users.decrementAndGet()
    }
}
