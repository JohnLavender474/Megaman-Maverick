package com.megaman.maverick.game.animations

import com.badlogic.gdx.utils.Array
import com.engine.common.extensions.gdxFilledArrayOf

data class AnimationDef(
    internal val rows: Int,
    internal val cols: Int,
    internal val durations: Array<Float>,
) {

    constructor(
        rows: Int = 1,
        cols: Int = 1,
        duration: Float = 1f,
    ) : this(rows, cols, gdxFilledArrayOf(rows * cols, duration))
}