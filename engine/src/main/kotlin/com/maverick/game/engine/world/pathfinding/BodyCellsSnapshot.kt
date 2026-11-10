package com.maverick.game.engine.world.pathfinding

import com.maverick.game.engine.world.IWorldBodies
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.container.SimpleGridWorldContainer

class BodyCellsSnapshot(ppm: Int) : IBodyCells {

    private val grid = SimpleGridWorldContainer(ppm)

    fun fill(source: IWorldBodies) {
        grid.clear()
        for (i in 0 until source.bodyCount) grid.addBody(source.getBody(i))
    }

    override fun getBodies(x: Int, y: Int, out: MutableCollection<IBody>) {
        grid.getBodies(x, y, out)
    }
}
