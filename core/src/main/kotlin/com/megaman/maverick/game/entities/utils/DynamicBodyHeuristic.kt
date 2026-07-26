package com.megaman.maverick.game.entities.utils

import com.mega.game.engine.common.objects.MutableOrderedSet
import com.mega.game.engine.pathfinding.heuristics.EuclideanHeuristic
import com.mega.game.engine.pathfinding.heuristics.IHeuristic
import com.mega.game.engine.world.body.IBody
import com.mega.game.engine.world.container.IWorldContainer
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.world.body.getEntity

class DynamicBodyHeuristic : IHeuristic {

    companion object {
        private const val CONTAINS_BLOCK_SCALAR = 5
    }

    var worldContainer: IWorldContainer? = null

    private val defaultHeuristic = EuclideanHeuristic()
    private val reusableBodySet = MutableOrderedSet<IBody>()

    private fun containsBlock(x: Int, y: Int): Boolean {
        val container = worldContainer ?: return false
        container.getBodies(x, y, reusableBodySet)

        var containsBlock = false
        for (body in reusableBodySet) if (body.getEntity().getType() == EntityType.BLOCK) {
            containsBlock = true
            break
        }
        reusableBodySet.clear()

        return containsBlock
    }

    override fun calculate(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        var cost = defaultHeuristic.calculate(x1, y1, x2, y2)
        if (containsBlock(x2, y2)) cost *= CONTAINS_BLOCK_SCALAR
        return cost
    }
}
