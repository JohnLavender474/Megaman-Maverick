package com.mega.game.engine.pathfinding

import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.diagnostics.RuntimeDiagnostics
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class TimeoutParams(val timeout: Long, val timeoutUnit: TimeUnit)

class AsyncPathfindingSystem(
    factory: IPathfinderFactory,
    private val timeoutParams: TimeoutParams = TimeoutParams(
        DEFAULT_TIMEOUT,
        DEFAULT_TIMEOUT_UNIT
    ),
    diagnostics: RuntimeDiagnostics? = null
) : AbstractPathfindingSystem(factory, diagnostics) {

    companion object {
        const val TAG = "PathfindingSystem"
        const val DEFAULT_TIMEOUT = 500L
        val DEFAULT_TIMEOUT_UNIT = TimeUnit.MILLISECONDS
    }

    private val executors = Executors.newCachedThreadPool()
    private val futureMap = OrderedMap<PathfindingComponent, Future<PathfinderResult>>()
    private val submitTimeMap = OrderedMap<PathfindingComponent, Long>()
    private val completedComponents = Queue<PathfindingComponent>()

    private val timeoutMillis
        get() = timeoutParams.timeoutUnit.toMillis(timeoutParams.timeout)

    override fun handleEntries(entries: OrderedMap<PathfindingComponent, IPathfinder>) {
        diagnostics?.beginEntry("AsyncPathfindingSystem.handleEntries")

        entries.forEach {
            val component = it.key
            val pathfinder = it.value
            if (!futureMap.containsKey(component)) {
                futureMap.put(component, executors.submit(pathfinder))
                submitTimeMap.put(component, System.currentTimeMillis())
            }
        }

        futureMap.forEach {
            val component = it.key
            val future = it.value
            when {
                future.isDone -> {
                    try {
                        component.currentPath = future.get()
                    } catch (e: Exception) {
                        future.cancel(true)
                        e.printStackTrace()
                    } finally {
                        submitTimeMap.remove(component)
                        completedComponents.addLast(component)
                    }
                }
                System.currentTimeMillis() - (submitTimeMap[component] ?: 0L) > timeoutMillis -> {
                    future.cancel(true)
                    submitTimeMap.remove(component)
                    completedComponents.addLast(component)
                }
            }
        }

        while (!completedComponents.isEmpty) {
            val completedComponent = completedComponents.removeFirst()
            futureMap.remove(completedComponent)
        }

        diagnostics?.endEntry()
    }

    override fun dispose() {
        if (!executors.isShutdown) executors.shutdown()
    }
}

