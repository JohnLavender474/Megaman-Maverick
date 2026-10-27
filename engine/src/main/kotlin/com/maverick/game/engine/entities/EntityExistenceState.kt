package com.maverick.game.engine.entities

enum class EntityExistenceState(val liveNow: Boolean, val liveAfterUpdate: Boolean) {
    UNSPAWNED(liveNow = false, liveAfterUpdate = false),
    SPAWN_QUEUED(liveNow = false, liveAfterUpdate = true),
    LIVE(liveNow = true, liveAfterUpdate = true),
    DESTROY_QUEUED(liveNow = true, liveAfterUpdate = false),
    RESPAWN_QUEUED(liveNow = true, liveAfterUpdate = true),
}
