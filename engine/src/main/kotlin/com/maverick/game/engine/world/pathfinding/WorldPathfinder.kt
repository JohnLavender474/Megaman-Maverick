package com.maverick.game.engine.world.pathfinding

import com.maverick.game.engine.common.objects.IntPair
import com.maverick.game.engine.pathfinding.IPathfinder
import com.maverick.game.engine.pathfinding.Pathfinder
import com.maverick.game.engine.pathfinding.Pathfinder.Companion.DEFAULT_MAX_DISTANCE
import com.maverick.game.engine.pathfinding.Pathfinder.Companion.DEFAULT_MAX_ITERATIONS
import com.maverick.game.engine.pathfinding.Pathfinder.Companion.DEFAULT_RETURN_BEST_PATH_ON_FAILURE
import com.maverick.game.engine.pathfinding.PathfinderResult
import com.maverick.game.engine.pathfinding.heuristics.IHeuristic
import com.maverick.game.engine.world.container.IWorldContainer

class WorldPathfinder(
    private val start: IntPair,
    private val target: IntPair,
    private val worldContainer: IWorldContainer?,
    private val worldWidth: Int,
    private val worldHeight: Int,
    private val allowDiagonal: Boolean,
    private val allowOutOfWorldBounds: Boolean,
    private val filter: ((Int, Int, IWorldContainer?) -> Boolean)?,
    private val heuristic: IHeuristic,
    private val maxIterations: Int = DEFAULT_MAX_ITERATIONS,
    private val maxDistance: Int = DEFAULT_MAX_DISTANCE,
    private val returnBestPathOnFailure: Boolean = DEFAULT_RETURN_BEST_PATH_ON_FAILURE
) : IPathfinder {

    private fun outOfWorldBounds(x: Int, y: Int) = x < 0 || y < 0 || x >= worldWidth || y >= worldHeight

    override fun call(): PathfinderResult {
        val pathfinder = Pathfinder(
            start,
            target,
            { x, y ->
                if (!allowOutOfWorldBounds && outOfWorldBounds(x, y)) false
                else if (filter?.invoke(x, y, worldContainer) == false) false
                else true
            },
            allowDiagonal,
            heuristic,
            maxIterations,
            maxDistance,
            returnBestPathOnFailure
        )
        return pathfinder.call()
    }
}
