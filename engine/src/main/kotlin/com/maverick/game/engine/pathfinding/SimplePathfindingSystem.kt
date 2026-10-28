package com.maverick.game.engine.pathfinding

import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics

class SimplePathfindingSystem(
    factory: IPathfinderFactory,
    diagnostics: RuntimeDiagnostics? = null,
    pathDebugConsumer: ((PathfinderResult) -> Unit)? = null
) : AbstractPathfindingSystem(factory, diagnostics, pathDebugConsumer) {

    override fun handleEntries(entries: OrderedMap<PathfindingComponent, IPathfinder>) {
        diagnostics?.beginEntry("SimplePathfindingSystem.handleEntries")

        entries.forEach {
            val component = it.key
            val pathfinder = it.value
            val result = pathfinder.call()
            component.currentPath = result
        }

        diagnostics?.endEntry()
    }
}
