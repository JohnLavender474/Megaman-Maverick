package com.megaman.maverick.game.animations

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.gdxFilledArrayOf

data class AnimationDef(
    internal val rows: Int,
    internal val cols: Int,
    internal val durations: Array<Float>,
    internal val loop: Boolean = true
) {

    constructor(
        rows: Int = 1,
        cols: Int = 1,
        duration: Float = 1f,
        loop: Boolean = true
    ) : this(rows, cols, gdxFilledArrayOf(rows * cols, duration), loop)
}
