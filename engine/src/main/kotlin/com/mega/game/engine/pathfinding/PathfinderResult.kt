package com.mega.game.engine.pathfinding

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.interfaces.IPropertizable
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.Properties


data class PathfinderResult(
    val path: Array<IntPair>?,
    val targetReached: Boolean,
    override val properties: Properties = Properties()
) : IPropertizable
