package com.mega.game.engine.pathfinding

import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.pathfinding.AsyncPathfindingSystem.Companion.DEFAULT_TIMEOUT
import com.mega.game.engine.pathfinding.AsyncPathfindingSystem.Companion.DEFAULT_TIMEOUT_UNIT
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


class TimeoutParams(val timeout: Long, val timeoutUnit: TimeUnit)


class AsyncPathfindingSystem(
    factory: IPathfinderFactory,
    private val timeoutParams: TimeoutParams = TimeoutParams(
        DEFAULT_TIMEOUT,
        DEFAULT_TIMEOUT_UNIT
    )
) : AbstractPathfindingSystem(factory) {

    companion object {
        const val TAG = "PathfindingSystem"
        const val DEFAULT_TIMEOUT = 10L
        val DEFAULT_TIMEOUT_UNIT = TimeUnit.MILLISECONDS
    }

    private val executors = Executors.newCachedThreadPool()
    private val futureMap = OrderedMap<PathfindingComponent, Future<PathfinderResult>>()
    private val completedComponents = Queue<PathfindingComponent>()

    override fun handleEntries(entries: OrderedMap<PathfindingComponent, IPathfinder>) {
        entries.forEach {
            val component = it.key
            val pathfinder = it.value
            if (!futureMap.containsKey(component)) {
                val future = executors.submit(pathfinder)
                futureMap.put(component, future)
            }
        }

        futureMap.forEach {
            val component = it.key
            val future = it.value
            if (future.isDone) {
                try {
                    val result = future.get(timeoutParams.timeout, timeoutParams.timeoutUnit)
                    component.currentPath = result
                    completedComponents.addLast(component)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    e.printStackTrace()
                    completedComponents.addLast(component)
                } catch (e: Exception) {
                    e.printStackTrace()
                    completedComponents.addLast(component)
                }

            }
        }

        while (!completedComponents.isEmpty) {
            val completedComponent = completedComponents.removeFirst()
            futureMap.remove(completedComponent)
        }
    }

    override fun dispose() {
        if (!executors.isShutdown) executors.shutdown()
    }
}

