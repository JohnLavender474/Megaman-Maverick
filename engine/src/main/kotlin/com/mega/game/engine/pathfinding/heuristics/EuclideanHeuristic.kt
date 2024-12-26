package com.mega.game.engine.pathfinding.heuristics

import kotlin.math.sqrt


class EuclideanHeuristic : IHeuristic {

    override fun calculate(x1: Int, y1: Int, x2: Int, y2: Int): Int =
        sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)).toFloat()).toInt()
}
