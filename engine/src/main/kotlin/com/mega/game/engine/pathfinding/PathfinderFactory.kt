package com.mega.game.engine.pathfinding


interface IPathfinderFactory {


    fun getPathfinder(params: PathfinderParams): IPathfinder
}