package com.megaman.maverick.game.world.wrap

import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.players.isPlayer
import com.maverick.game.engine.world.WorldWrap
import com.megaman.maverick.game.entities.contracts.IProjectileEntity

// TODO: no per-instance override yet; it comes with its first caller (item 109 phase 2)
// TODO: enemies, items and effects do not wrap yet (item 109 phase 2)
object WrapPolicy {

    const val SHOT_MAX_WRAPS = 1

    fun wraps(entity: IGameEntity): Boolean = entity.isPlayer || entity is IProjectileEntity

    // TODO: a weapon cannot override the shot cap yet (item 109 phase 2)
    fun maxWraps(entity: IGameEntity): Int = when {
        entity.isPlayer -> WorldWrap.UNLIMITED_WRAPS
        entity is IProjectileEntity -> SHOT_MAX_WRAPS
        else -> 0
    }
}
