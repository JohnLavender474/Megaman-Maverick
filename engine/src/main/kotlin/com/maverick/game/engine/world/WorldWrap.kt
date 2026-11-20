package com.maverick.game.engine.world

import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.IGameEntity

class WorldWrap(
    val region: (out: GameRectangle) -> GameRectangle?,
    val wraps: (entity: IGameEntity) -> Boolean,
    val maxWraps: (entity: IGameEntity) -> Int
) {

    companion object {
        const val UNLIMITED_WRAPS = Int.MAX_VALUE

        fun seamShift(
            minX: Float,
            maxX: Float,
            region: GameRectangle,
            wrapCount: Int,
            lastWrapDx: Float,
            maxWraps: Int
        ): Float {
            val shift = when {
                minX < region.getX() -> region.getWidth()
                maxX > region.getMaxX() -> -region.getWidth()
                else -> return 0f
            }
            return if (wrapCount < maxWraps || shift == -lastWrapDx) shift else 0f
        }
    }
}
