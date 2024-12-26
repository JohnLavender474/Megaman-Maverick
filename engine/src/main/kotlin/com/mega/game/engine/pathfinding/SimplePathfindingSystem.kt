package com.mega.game.engine.pathfinding

import com.badlogic.gdx.utils.OrderedMap


class SimplePathfindingSystem(factory: IPathfinderFactory) : AbstractPathfindingSystem(factory) {


    override fun handleEntries(entries: OrderedMap<PathfindingComponent, IPathfinder>) {
        entries.forEach {
            val component = it.key
            val pathfinder = it.value
            val result = pathfinder.call()
            component.currentPath = result
        }
    }
}
