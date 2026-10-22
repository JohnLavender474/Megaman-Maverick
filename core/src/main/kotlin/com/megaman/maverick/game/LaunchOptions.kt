package com.megaman.maverick.game

data class LaunchOptions(
    val screen: String? = null,
    val level: String? = null,
    val room: String? = null,
    val logTags: String? = null,
    val autoPerf: Boolean = true
)
