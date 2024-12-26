package com.mega.game.engine.pathfinding.heuristics

import kotlin.math.abs


class OctileHeuristic(
    private val orthogonalCost: Int = 1,
    private val diagonalCost: Int = 1
) : IHeuristic {


    override fun calculate(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        val dx = abs(x1 - x2)
        val dy = abs(y1 - y2)
        return orthogonalCost * (dx + dy) + (diagonalCost - 2 * orthogonalCost) * minOf(dx, dy)
    }
}
