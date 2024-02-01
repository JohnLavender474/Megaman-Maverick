package com.megaman.maverick.game.entities.megaman.constants

/** Represents a heart tank that Megaman can collect. */
enum class MegaHeartTank {
    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H;

    companion object {
        /** The amount of health that a heart tank gives. */
        const val HEALTH_BUMP = 2

        /** Gets the [MegaHeartTank] with the given string representation. */
        fun get(s: String): MegaHeartTank {
            return when (s) {
                "A" -> A
                "B" -> B
                "C" -> C
                "D" -> D
                "E" -> E
                "F" -> F
                "G" -> G
                "H" -> H
                else -> throw IllegalArgumentException("Invalid heart tank: $s")
            }
        }
    }
}
