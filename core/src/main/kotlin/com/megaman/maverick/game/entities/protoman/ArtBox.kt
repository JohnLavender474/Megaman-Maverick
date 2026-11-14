package com.megaman.maverick.game.entities.protoman

internal class ArtBox(val left: Int, val top: Int, val right: Int, val bottom: Int) {

    val width: Int
        get() = right - left + 1

    val height: Int
        get() = bottom - top + 1

    val centerX: Float
        get() = (left + right + 1) / 2f

    val centerY: Float
        get() = (top + bottom + 1) / 2f
}
