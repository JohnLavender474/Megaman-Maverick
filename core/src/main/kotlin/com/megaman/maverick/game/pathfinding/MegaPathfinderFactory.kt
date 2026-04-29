package com.megaman.maverick.game.pathfinding

import com.mega.game.engine.pathfinding.IPathfinder
import com.mega.game.engine.pathfinding.IPathfinderFactory
import com.mega.game.engine.pathfinding.PathfinderParams
import com.mega.game.engine.pathfinding.heuristics.EuclideanHeuristic
import com.mega.game.engine.pathfinding.heuristics.IHeuristic
import com.mega.game.engine.world.pathfinding.WorldPathfinder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame

class MegaPathfinderFactory(private val game: MegamanMaverickGame): IPathfinderFactory {

    override fun getPathfinder(params: PathfinderParams): IPathfinder {
        val tiledMapResult = game.getTiledMapLoadResult()
        return WorldPathfinder(
            start = params.startCoordinateSupplier(),
            target = params.targetCoordinateSupplier(),
            worldWidth = tiledMapResult.worldWidth,
            worldHeight = tiledMapResult.worldHeight,
            allowDiagonal = params.allowDiagonal(),
            allowOutOfWorldBounds = params.getOrDefaultProperty(
                ConstKeys.ALLOW_OUT_OF_BOUNDS,
                true,
                Boolean::class
            ),
            filter = params.filter,
            heuristic = params.getOrDefaultProperty(
                ConstKeys.HEURISTIC,
                EuclideanHeuristic(),
                IHeuristic::class
            ),
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
