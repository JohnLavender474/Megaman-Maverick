package com.megaman.maverick.game.entities.megaman.constants


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
        const val HEALTH_BUMP = 2

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
