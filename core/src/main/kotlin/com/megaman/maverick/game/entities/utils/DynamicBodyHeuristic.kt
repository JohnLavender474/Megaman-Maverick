package com.megaman.maverick.game.entities.utils

import com.mega.game.engine.common.objects.MutableOrderedSet
import com.mega.game.engine.pathfinding.heuristics.EuclideanHeuristic
import com.mega.game.engine.pathfinding.heuristics.IHeuristic
import com.mega.game.engine.world.body.IBody
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.world.body.getEntity

class DynamicBodyHeuristic(private val game: MegamanMaverickGame) : IHeuristic {

    companion object {
        private const val CONTAINS_BLOCK_SCALAR = 5
    }

    private val defaultHeuristic = EuclideanHeuristic()
    private val reusableBodySet = MutableOrderedSet<IBody>()

    private fun containsBlock(x: Int, y: Int): Boolean {
        game.getWorldContainer()!!.getBodies(x, y, reusableBodySet)

        for (body in reusableBodySet) if (body.getEntity().getType() == EntityType.BLOCK) {
            reusableBodySet.clear()
            return true
        }

        reusableBodySet.clear()

        return false
    }

    override fun calculate(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        var cost = defaultHeuristic.calculate(x1, y1, x2, y2)
        if (containsBlock(x2, y2)) cost *= CONTAINS_BLOCK_SCALAR
        return cost
    }
}
