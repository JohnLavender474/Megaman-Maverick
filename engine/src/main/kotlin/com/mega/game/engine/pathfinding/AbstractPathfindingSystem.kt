package com.mega.game.engine.pathfinding

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.diagnostics.RuntimeDiagnostics
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem


abstract class AbstractPathfindingSystem(
    private val factory: IPathfinderFactory,
    protected val diagnostics: RuntimeDiagnostics? = null
):
    GameSystem(PathfindingComponent::class) {

    companion object {
        const val TAG = "AbstractPathfindingSystem"
    }

    protected abstract fun handleEntries(entries: OrderedMap<PathfindingComponent, IPathfinder>)

    final override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("AbstractPathfindingSystem")

        val entries = OrderedMap<PathfindingComponent, IPathfinder>()
        entities.forEach { entity ->
            try {
                val component = entity.getComponent(PathfindingComponent::class) ?: return@forEach

                // Consume the current path
                val currentPath = component.currentPath
                if (currentPath != null) component.consumer(currentPath)

                // Update the interval timer
                val updateIntervalTimer = component.intervalTimer
                updateIntervalTimer.update(delta)
                if (!updateIntervalTimer.isFinished()) return@forEach

                // Reset the update interval timer
                updateIntervalTimer.reset()

                // Check if the component should update
                if (!component.doUpdate()) return@forEach

                // Create the pathfinder and add it to the entries map
                val pathfinder = factory.getPathfinder(component.params)
                entries.put(component, pathfinder)
            } catch (e: Exception) {
                throw Exception("Exception occured while processing pathfinding for entity: $entity", e)
            }
        }
        handleEntries(entries)

        diagnostics?.endEntry()
    }
}
