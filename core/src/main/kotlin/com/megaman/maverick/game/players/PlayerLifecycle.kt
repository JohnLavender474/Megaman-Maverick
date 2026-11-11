package com.megaman.maverick.game.players

import com.maverick.game.engine.common.GameLogger
import com.megaman.maverick.game.entities.utils.isOutsideLevel

private const val TAG = "PlayerLifecycle"

private const val OUTSIDE_LEVEL_X_OFFSET = 1.5f
private const val OUTSIDE_LEVEL_Y_OFFSET = 1.5f

fun MegaPlayer.killPlayerIfOutsideLevel() {
    if (isOutsideLevel(services.levelState, OUTSIDE_LEVEL_X_OFFSET, OUTSIDE_LEVEL_Y_OFFSET)) {
        GameLogger.error(TAG, "The player is outside the level bounds, kill it: $this")
        destroy()
    }
}
