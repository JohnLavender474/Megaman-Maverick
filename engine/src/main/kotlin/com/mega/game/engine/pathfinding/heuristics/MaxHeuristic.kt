package com.mega.game.engine.pathfinding.heuristics

import kotlin.math.abs


class MaxHeuristic : IHeuristic {

    override fun calculate(x1: Int, y1: Int, x2: Int, y2: Int): Int = maxOf(abs(x1 - x2), abs(y1 - y2))
}
