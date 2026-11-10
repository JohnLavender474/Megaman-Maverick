package com.megaman.maverick.game.pathfinding

import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.world.body.IBody
import com.maverick.game.engine.world.pathfinding.IBodyCells
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.world.body.getEntity

object BlockCells {

    private class Scratch {
        val cell = GameRectangle()
        val bounds = GameRectangle()
        val bodies = ArrayList<IBody>()
    }

    private val scratch = ThreadLocal.withInitial { Scratch() }

    fun containsBlock(cells: IBodyCells, x: Int, y: Int): Boolean {
        val bodies = scratch.get().bodies
        bodies.clear()
        cells.getBodies(x, y, bodies)
        for (i in 0 until bodies.size) if (bodies[i].getEntity().getType() == EntityType.BLOCK) return true
        return false
    }

    fun containsBlockOverlappingCell(cells: IBodyCells, x: Int, y: Int): Boolean {
        val scratch = scratch.get()
        val cell = scratch.cell.set(
            x * ConstVals.PPM.toFloat(),
            y * ConstVals.PPM.toFloat(),
            ConstVals.PPM.toFloat(),
            ConstVals.PPM.toFloat()
        )
        val bodies = scratch.bodies
        bodies.clear()
        cells.getBodies(x, y, bodies)
        for (i in 0 until bodies.size) {
            val body = bodies[i]
            if (body.getEntity().getType() == EntityType.BLOCK && body.getBounds(scratch.bounds).overlaps(cell))
                return true
        }
        return false
    }
}
