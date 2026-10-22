package com.maverick.game.engine.pathfinding

import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.IGameComponent

class PathfindingComponent(
    var params: PathfinderParams,
    var consumer: (PathfinderResult) -> Unit,
    var doUpdate: () -> Boolean = { true },
    var intervalTimer: Timer = Timer(DEFAULT_UPDATE_INTERVAL)
) : IGameComponent {

    companion object {
        const val DEFAULT_UPDATE_INTERVAL = 0.1f
    }

    internal var currentPath: PathfinderResult? = null
}
