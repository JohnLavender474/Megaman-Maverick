package com.maverick.game.engine.pathfinding

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.interfaces.IPropertizable
import com.maverick.game.engine.common.objects.IntPair
import com.maverick.game.engine.common.objects.Properties


data class PathfinderResult(
    val path: Array<IntPair>?,
    val targetReached: Boolean,
    override val properties: Properties = Properties()
) : IPropertizable
