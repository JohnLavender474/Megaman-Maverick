package com.maverick.game.engine.pathfinding

import java.util.concurrent.Callable

interface IPathfinder : Callable<PathfinderResult>
