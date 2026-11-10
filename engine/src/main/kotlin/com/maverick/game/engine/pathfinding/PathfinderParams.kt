package com.maverick.game.engine.pathfinding

import com.maverick.game.engine.world.pathfinding.IBodyCells
import com.maverick.game.engine.common.interfaces.IPropertizable
import com.maverick.game.engine.common.objects.IntPair
import com.maverick.game.engine.common.objects.Properties

class PathfinderParams(
    var startCoordinateSupplier: () -> IntPair,
    var targetCoordinateSupplier: () -> IntPair,
    var allowDiagonal: () -> Boolean,
    var filter: (Int, Int, IBodyCells?) -> Boolean,
    override val properties: Properties = Properties()
) : IPropertizable
