package com.maverick.game.engine.pathfinding

interface IPathfinderFactory {

    fun getPathfinder(params: PathfinderParams): IPathfinder
}
