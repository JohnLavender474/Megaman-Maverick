package com.mega.game.engine.pathfinding

import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.Properties


class PathfinderParams(
    var startCoordinateSupplier: () -> IntPair,
    var targetCoordinateSupplier: () -> IntPair,
    var allowDiagonal: () -> Boolean,
    var filter: (IntPair) -> Boolean,
    override val properties: Properties = Properties()
) : IPropertizable
