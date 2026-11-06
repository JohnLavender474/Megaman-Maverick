package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.IGameComponent

enum class BossPhase { ENTRANCE, INTRO, FIGHT }

// TODO: 048 step 6 moves the boss hooks (isReady, preReady, onReady, onEndBossSpawnEvent) into this component
class BossComponent : IGameComponent {

    var phase = BossPhase.ENTRANCE

    override fun reset() {
        phase = BossPhase.ENTRANCE
    }
}
