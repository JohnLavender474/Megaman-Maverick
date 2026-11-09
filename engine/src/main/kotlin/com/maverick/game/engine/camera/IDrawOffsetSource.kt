package com.maverick.game.engine.camera

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.world.IDrawOffsets

fun interface IDrawOffsetSource {

    fun getDrawOffset(offsets: IDrawOffsets, out: Vector2): Vector2
}
