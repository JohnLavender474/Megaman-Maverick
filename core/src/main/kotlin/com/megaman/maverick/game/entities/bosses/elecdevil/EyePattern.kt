package com.megaman.maverick.game.entities.bosses.elecdevil

enum class EyePattern(val color: String) {
    FAN("blue"),
    RING("red"),
    STALL("green"),
    BOUNCE("yellow");

    fun regionKey(base: String) = "${base}_$color"
}
