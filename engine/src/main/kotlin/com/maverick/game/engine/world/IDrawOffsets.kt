package com.maverick.game.engine.world

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.world.body.IBody

interface IDrawOffsets {

    fun getDrawOffset(body: IBody, out: Vector2): Vector2
}
