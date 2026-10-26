package com.megaman.maverick.game.utils.misc

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.players.body
import com.megaman.maverick.game.players.direction
import com.megaman.maverick.game.players.facing
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.isSensing

object FacingUtils {

    const val TAG = "StandardFacingSetter"

    fun getPreferredFacingFor(entity: MegaGameEntity): Facing {
        val target = entity.targetPlayer

        entity as IBodyEntity
        entity as IFaceable

        try {
            // assert facing is already defined
            entity.facing.value

            val megamanBody = target.body.getBounds()
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

            return facing ?: if (target.body.getX() < entity.body.getX()) Facing.LEFT else Facing.RIGHT
        } catch (e: Exception) {
            GameLogger.log(TAG, "get(): ran into error while trying to get preferred facing value", e)

            return if (target.body.getX() < entity.body.getX()) Facing.LEFT else Facing.RIGHT
        }
    }

    fun setFacingOf(entity: MegaGameEntity, onChangeFacing: ((current: Facing, previous: Facing?) -> Unit)? = null) {
        entity as IFaceable

        val previous: Facing? = try {
            entity.facing
        } catch (_: Exception) {
            null
        }

        val current = getPreferredFacingFor(entity)

        if (previous != current) {
            entity.facing = current

            onChangeFacing?.invoke(current, previous)
        }
    }

    fun isFacingBlock(entity: IBodyEntity): Boolean {
        entity as IFaceable

        return (entity.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && entity.isFacing(Facing.LEFT)) ||
            (entity.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && entity.isFacing(Facing.RIGHT))
    }

    fun isBackTouchingBlock(entity: IBodyEntity): Boolean {
        entity as IFaceable

        return (entity.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && entity.isFacing(Facing.RIGHT)) ||
            (entity.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && entity.isFacing(Facing.LEFT))
    }
}
