package com.mega.game.engine.common.shapes

import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.utils.Array


fun Polyline.toGameLines(): Array<GameLine> {
    val lines = Array<GameLine>()
    for (i in 0 until this.vertices.size - 2 step 2) {
        val line = GameLine(this.vertices[i], this.vertices[i + 1], this.vertices[i + 2], this.vertices[i + 3])
        lines.add(line)
    }
    return lines
}