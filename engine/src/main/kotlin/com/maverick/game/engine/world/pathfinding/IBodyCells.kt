package com.maverick.game.engine.world.pathfinding

import com.maverick.game.engine.world.body.IBody

interface IBodyCells {

    fun getBodies(x: Int, y: Int, out: MutableCollection<IBody>)
}
