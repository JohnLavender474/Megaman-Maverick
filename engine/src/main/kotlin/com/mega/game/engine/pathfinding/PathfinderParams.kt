package com.mega.game.engine.pathfinding

import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.world.container.IWorldContainer


class PathfinderParams(
    var startCoordinateSupplier: () -> IntPair,
    var targetCoordinateSupplier: () -> IntPair,
    var allowDiagonal: () -> Boolean,
    var filter: (IntPair, IWorldContainer?) -> Boolean,
    override val properties: Properties = Properties()
) : IPropertizable
