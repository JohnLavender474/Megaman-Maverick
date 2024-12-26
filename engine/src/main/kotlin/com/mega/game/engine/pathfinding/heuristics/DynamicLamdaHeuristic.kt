package com.mega.game.engine.pathfinding.heuristics


class DynamicLambdaHeuristic(private val distanceLambda: (x1: Int, y1: Int, x2: Int, y2: Int) -> Int) : IHeuristic {


    override fun calculate(x1: Int, y1: Int, x2: Int, y2: Int): Int = distanceLambda(x1, y1, x2, y2)
}
