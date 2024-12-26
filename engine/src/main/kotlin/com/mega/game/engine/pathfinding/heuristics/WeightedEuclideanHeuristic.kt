package com.mega.game.engine.pathfinding.heuristics

import kotlin.math.sqrt


class WeightedEuclideanHeuristic(private val weight: Float = 1f) : IHeuristic {

    override fun calculate(x1: Int, y1: Int, x2: Int, y2: Int): Int =
        (weight * sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)).toFloat())).toInt()
}
