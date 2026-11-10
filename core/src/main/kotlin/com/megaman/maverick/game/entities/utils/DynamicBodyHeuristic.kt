package com.megaman.maverick.game.entities.utils

import com.megaman.maverick.game.pathfinding.BlockCells
import com.maverick.game.engine.pathfinding.heuristics.EuclideanHeuristic
import com.maverick.game.engine.pathfinding.heuristics.IHeuristic
import com.maverick.game.engine.world.pathfinding.IBodyCells

class DynamicBodyHeuristic : IHeuristic {

    companion object {
        private const val CONTAINS_BLOCK_SCALAR = 5
    }

    var cells: IBodyCells? = null

    private val defaultHeuristic = EuclideanHeuristic()

    private fun containsBlock(x: Int, y: Int): Boolean {
        val cells = cells ?: return false
        return BlockCells.containsBlock(cells, x, y)
    }

    override fun calculate(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        var cost = defaultHeuristic.calculate(x1, y1, x2, y2)
        if (containsBlock(x2, y2)) cost *= CONTAINS_BLOCK_SCALAR
        return cost
    }
}
