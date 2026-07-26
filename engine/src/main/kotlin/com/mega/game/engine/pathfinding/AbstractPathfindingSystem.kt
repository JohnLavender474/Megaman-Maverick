package com.mega.game.engine.pathfinding

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.diagnostics.RuntimeDiagnostics
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem

abstract class AbstractPathfindingSystem(
    private val factory: IPathfinderFactory,
    protected val diagnostics: RuntimeDiagnostics? = null
): GameSystem(PathfindingComponent::class) {

    companion object {
        const val TAG = "AbstractPathfindingSystem"
    }

    private val entries = OrderedMap<PathfindingComponent, IPathfinder>()

    protected abstract fun handleEntries(entries: OrderedMap<PathfindingComponent, IPathfinder>)

    protected open fun shouldPerformPathfinding(component: PathfindingComponent) = true

    final override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("AbstractPathfindingSystem")

        entries.clear()
        entities.forEach { entity ->
            try {
                val component = entity.getComponent(PathfindingComponent::class) ?: return@forEach

                val currentPath = component.currentPath
                if (currentPath != null) component.consumer(currentPath)

                val updateIntervalTimer = component.intervalTimer
                updateIntervalTimer.update(delta)
                if (!updateIntervalTimer.isFinished()) return@forEach
                updateIntervalTimer.reset()

                if (!component.doUpdate()) return@forEach
                if (!shouldPerformPathfinding(component)) return@forEach

                val pathfinder = factory.getPathfinder(component.params)
                entries.put(component, pathfinder)
            } catch (e: Exception) {
                throw Exception("Exception occurred while processing pathfinding for entity: $entity", e)
            }
        }

        handleEntries(entries)
        entries.clear()

        diagnostics?.endEntry()
    }
}
