package com.megaman.maverick.game.state

import com.maverick.game.engine.common.enums.Direction

enum class ViewEdgeRule { NONE, WALL, BOUNCE, WARP, KILL }

data class ViewEdgeRules(
    val left: ViewEdgeRule = ViewEdgeRule.WALL,
    val right: ViewEdgeRule = ViewEdgeRule.WALL,
    val top: ViewEdgeRule = ViewEdgeRule.NONE,
    val bottom: ViewEdgeRule = ViewEdgeRule.NONE,
    val delay: Float = 0f,
    val damage: Int = 0,
    val warpCap: Float = 3f
) {

    fun of(edge: Direction) = when (edge) {
        Direction.LEFT -> left
        Direction.RIGHT -> right
        Direction.UP -> top
        Direction.DOWN -> bottom
    }
}
