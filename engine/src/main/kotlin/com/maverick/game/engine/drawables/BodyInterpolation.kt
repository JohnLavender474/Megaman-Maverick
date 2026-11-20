package com.maverick.game.engine.drawables

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.world.IDrawOffsets
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.body.IBody
import kotlin.math.abs

class BodyInterpolation(
    private val world: WorldSystem,
    var interpolation: Boolean
) : IDrawOffsets {

    override fun getDrawOffset(body: IBody, out: Vector2): Vector2 {
        if (!interpolation || body.stepSerial != world.stepSerial) return out.setZero()
        body.getStepMove(out)
        // TODO: a teleport inside a step that moves less than 1 tile and does not call restartStep is drawn as a
        //  slide for 1 frame; PipePortal calls it, other teleports are outside the steps (item 134, 134b audit)
        if (abs(out.x) > world.ppm || abs(out.y) > world.ppm) return out.setZero()
        return out.scl(world.alpha - 1f)
    }
}
