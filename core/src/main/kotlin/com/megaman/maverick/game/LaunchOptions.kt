package com.megaman.maverick.game

data class LaunchOptions(
    val screen: String? = null,
    val level: String? = null,
    val room: String? = null,
    val logTags: String? = null,
    val weapons: String? = null,
    val heartTanks: String? = null,
    val healthTanks: String? = null,
    val autoPerf: Boolean = true,
    val players: String? = null,
    val mode: String? = null,
    val viewEdges: String? = null,
    val saveFile: String? = null,
    val difficulty: String? = null,
    val randSeed: String? = null
)
