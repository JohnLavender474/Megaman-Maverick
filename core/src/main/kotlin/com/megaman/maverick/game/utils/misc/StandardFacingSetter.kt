package com.megaman.maverick.game.utils.misc

import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman

object StandardFacingSetter {

    fun set(entity: MegaGameEntity) {
        val megaman = entity.megaman

        entity as IBodyEntity
        val body = entity.body

        entity as IFaceable
        try {
            // assert facing is already defined
            entity.facing.value

            when {
                megaman.body.getMaxX() < body.getX() -> entity.facing = Facing.LEFT
                megaman.body.getX() > body.getMaxX() -> entity.facing = Facing.RIGHT
            }
        } catch (_: Exception) {
            // assign starting value if facing is not defined
            entity.facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
        }
    }
}
