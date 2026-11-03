package com.megaman.maverick.game.fights

fun interface Lease {

    fun release()
}

enum class FightLease { WORLD_HOLD, HEALTH_BAR }
