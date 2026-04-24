package com.mega.game.engine.pathfinding

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.diagnostics.RuntimeDiagnostics

class SimplePathfindingSystem(
    factory: IPathfinderFactory,
    diagnostics: RuntimeDiagnostics? = null
) : AbstractPathfindingSystem(factory, diagnostics) {

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
