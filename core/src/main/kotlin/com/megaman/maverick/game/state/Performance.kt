package com.megaman.maverick.game.state

enum class Performance(val fps: Int, val fixedStep: Float) {
    VERY_LOW(30, 1f / 75f),
    LOW(45, 1f / 112.5f),
    MEDIUM(60, 1f / 150f),
    HIGH(90, 1f / 225f)
}
