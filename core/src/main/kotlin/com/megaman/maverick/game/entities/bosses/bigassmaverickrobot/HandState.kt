package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

enum class HandState(val damager: Boolean, val platform: Boolean, val rocket: Boolean) {
    FOLLOW(damager = true, platform = false, rocket = false),
    MOVE_TO_ORIGIN(damager = true, platform = false, rocket = false),
    REVEAL_ORBS(damager = true, platform = false, rocket = false),
    IDLE(damager = false, platform = true, rocket = false),
    WIND_UP(damager = true, platform = false, rocket = false),
    LAUNCH(damager = true, platform = false, rocket = true),
    RECOIL(damager = true, platform = false, rocket = true),
    RISE(damager = true, platform = false, rocket = false),
    HANG(damager = true, platform = false, rocket = false),
    FALL(damager = true, platform = false, rocket = true),
    REST(damager = false, platform = true, rocket = false),
    CLAP_ALIGN(damager = true, platform = false, rocket = false),
    CLAP_LOCK(damager = true, platform = false, rocket = false),
    CLAP_CLOSE(damager = true, platform = false, rocket = true),
    CLAP_IMPACT(damager = true, platform = false, rocket = false),
    RETURN(damager = false, platform = true, rocket = false),
    DEFEATED(damager = false, platform = false, rocket = false)
}

enum class HandAttack { PUNCH, SLAM, PINCER_CLAP }
