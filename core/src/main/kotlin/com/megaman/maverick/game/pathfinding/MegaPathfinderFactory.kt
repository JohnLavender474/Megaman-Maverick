package com.megaman.maverick.game.pathfinding

import com.badlogic.gdx.Gdx
import com.maverick.game.engine.pathfinding.IPathfinder
import com.maverick.game.engine.pathfinding.IPathfinderFactory
import com.maverick.game.engine.pathfinding.PathfinderParams
import com.maverick.game.engine.pathfinding.heuristics.EuclideanHeuristic
import com.maverick.game.engine.pathfinding.heuristics.IHeuristic
import com.maverick.game.engine.world.container.IWorldContainer
import com.maverick.game.engine.world.pathfinding.WorldPathfinder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.utils.DynamicBodyHeuristic
import com.megaman.maverick.game.levels.LevelState

class MegaPathfinderFactory(private val levelState: LevelState): IPathfinderFactory {

    private val defaultHeuristic = EuclideanHeuristic()

    private var snapshotFrameId = -1L
    private var snapshot: IWorldContainer? = null
    private var snapshotSource: IWorldContainer? = null

    private fun getWorldContainerSnapshot(): IWorldContainer? {
        val frameId = Gdx.graphics.frameId
        val source = levelState.worldContainer
        if (frameId != snapshotFrameId || source !== snapshotSource) {
            val container = levelState.worldContainer
            snapshot = container?.copy()
            snapshotSource = container
            snapshotFrameId = frameId
        }
        return snapshot
    }

    override fun getPathfinder(params: PathfinderParams): IPathfinder {
        val worldContainer = getWorldContainerSnapshot()

        val heuristic = params.getOrDefaultProperty(ConstKeys.HEURISTIC, defaultHeuristic, IHeuristic::class)
        if (heuristic is DynamicBodyHeuristic) heuristic.worldContainer = worldContainer

        val tiledMapResult = levelState.tiledMapLoadResult!!

        return WorldPathfinder(
            start = params.startCoordinateSupplier(),
            target = params.targetCoordinateSupplier(),
            worldContainer = worldContainer,
            worldWidth = tiledMapResult.worldWidth,
            worldHeight = tiledMapResult.worldHeight,
            allowDiagonal = params.allowDiagonal(),
            allowOutOfWorldBounds = params.getOrDefaultProperty(
                ConstKeys.ALLOW_OUT_OF_BOUNDS,
                true,
                Boolean::class
            ),
            filter = params.filter,
            heuristic = heuristic,
            maxIterations = params.getOrDefaultProperty(
                ConstKeys.ITERATIONS,
                ConstVals.DEFAULT_PATHFINDING_MAX_ITERATIONS,
                Int::class
            ),
            maxDistance = params.getOrDefaultProperty(
                ConstKeys.DISTANCE,
                ConstVals.DEFAULT_PATHFINDING_MAX_DISTANCE,
                Int::class
            ),
            returnBestPathOnFailure = params.getOrDefaultProperty(
                ConstKeys.DEFAULT,
                ConstVals.DEFAULT_RETURN_BEST_PATH,
                Boolean::class
            )
        )
    }
}
