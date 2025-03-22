package com.megaman.maverick.game.utils.misc

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.isSensing

object FacingUtils {

    const val TAG = "StandardFacingSetter"

    fun getPreferredFacingFor(entity: MegaGameEntity): Facing {
        val megaman = entity.megaman

        entity as IBodyEntity
        entity as IFaceable

        try {
            // assert facing is already defined
            entity.facing.value

            val megamanBody = megaman.body.getBounds()
            val thisBody = entity.body.getBounds()

            val direction = if (entity is IDirectional) entity.direction else Direction.UP

            var facing: Facing? = null

            when (direction) {
                Direction.UP -> when {
                    megamanBody.getMaxX() < thisBody.getX() -> facing = Facing.LEFT
                    megamanBody.getX() > thisBody.getMaxX() -> facing = Facing.RIGHT
                }

                Direction.DOWN -> when {
                    megamanBody.getMaxX() < thisBody.getX() -> facing = Facing.RIGHT
                    megamanBody.getX() > thisBody.getMaxX() -> facing = Facing.LEFT
                }

                Direction.LEFT -> when {
                    megamanBody.getMaxY() < thisBody.getY() -> facing = Facing.LEFT
                    megamanBody.getY() > thisBody.getMaxY() -> facing = Facing.RIGHT
                }

                Direction.RIGHT -> when {
                    megamanBody.getMaxY() < thisBody.getY() -> facing = Facing.RIGHT
                    megamanBody.getY() > thisBody.getMaxY() -> facing = Facing.LEFT
                }
            }

            return facing ?: if (megaman.body.getX() < entity.body.getX()) Facing.LEFT else Facing.RIGHT
        } catch (e: Exception) {
            GameLogger.error(TAG, "get(): ran into error while trying to get preferred facing value", e)

            return if (megaman.body.getX() < entity.body.getX()) Facing.LEFT else Facing.RIGHT
        }
    }

    fun setFacingOf(entity: MegaGameEntity) {
        entity as IFaceable
        entity.facing = getPreferredFacingFor(entity)
    }

    fun isFacingBlock(entity: IBodyEntity): Boolean {
        entity as IFaceable
        return (entity.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && entity.isFacing(Facing.LEFT)) ||
            (entity.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && entity.isFacing(Facing.RIGHT))
    }
}
