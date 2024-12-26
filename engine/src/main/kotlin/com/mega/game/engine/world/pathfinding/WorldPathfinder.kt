package com.mega.game.engine.world.pathfinding

import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.pathfinding.IPathfinder
import com.mega.game.engine.pathfinding.Pathfinder
import com.mega.game.engine.pathfinding.Pathfinder.Companion.DEFAULT_MAX_DISTANCE
import com.mega.game.engine.pathfinding.Pathfinder.Companion.DEFAULT_MAX_ITERATIONS
import com.mega.game.engine.pathfinding.Pathfinder.Companion.DEFAULT_RETURN_BEST_PATH_ON_FAILURE
import com.mega.game.engine.pathfinding.PathfinderResult
import com.mega.game.engine.pathfinding.heuristics.IHeuristic
import java.util.function.Predicate

class WorldPathfinder(
    private val start: IntPair,
    private val target: IntPair,
    private val worldWidth: Int,
    private val worldHeight: Int,
    private val allowDiagonal: Boolean,
    private val allowOutOfWorldBounds: Boolean,
    private val filter: ((IntPair) -> Boolean)?,
    private val heuristic: IHeuristic,
    private val maxIterations: Int = DEFAULT_MAX_ITERATIONS,
    private val maxDistance: Int = DEFAULT_MAX_DISTANCE,
    private val returnBestPathOnFailure: Boolean = DEFAULT_RETURN_BEST_PATH_ON_FAILURE
) : IPathfinder {

    constructor(
        start: IntPair,
        target: IntPair,
        worldWidth: Int,
        worldHeight: Int,
        allowDiagonal: Boolean,
        allowOutOfWorldBounds: Boolean,
        filter: Predicate<IntPair>?,
        heuristic: IHeuristic,
        maxIterations: Int,
        maxDistance: Int,
        returnBestPathOnFailure: Boolean
    ) : this(
        start,
        target,
        worldWidth,
        worldHeight,
        allowDiagonal,
        allowOutOfWorldBounds,
        filter?.let { { coordinate: IntPair -> it.test(coordinate) } },
        heuristic,
        maxIterations,
        maxDistance,
        returnBestPathOnFailure
    )

    private fun outOfWorldBounds(coordinate: IntPair) =
        coordinate.x < 0 || coordinate.y < 0 || coordinate.x >= worldWidth || coordinate.y >= worldHeight

    override fun call(): PathfinderResult {
        val pathfinder = Pathfinder(
            start,
            target,
            {
                if (!allowOutOfWorldBounds && outOfWorldBounds(it)) false
                else if (filter?.invoke(it) == false) false
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
