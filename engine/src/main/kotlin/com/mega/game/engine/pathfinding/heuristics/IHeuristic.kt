package com.mega.game.engine.pathfinding.heuristics


interface IHeuristic {


    fun calculate(x1: Int, y1: Int, x2: Int, y2: Int): Int
}
