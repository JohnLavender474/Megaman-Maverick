package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.behaviors.FunctionalBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.stopMomentum
import com.megaman.maverick.game.entities.state.teleporting
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.world.body.isSensingAll
import com.megaman.maverick.game.world.body.isSensingAny

private const val TAG = "PlayerJump"

class JumpParams(
    val canStart: () -> Boolean,
    val blocked: () -> Boolean,
    val jumpSpeed: () -> Float,
    val wallJumpSpeed: () -> Float,
    val wallJumpHorizontal: Float,
    val wallJumpAllowed: () -> Boolean,
    val ignoreRelease: () -> Boolean,
    val onJump: () -> Unit
)

fun <T> T.playerJumpBehavior(params: JumpParams): IBehavior
    where T : AbstractHealthEntity, T : IBodyEntity, T : IFaceable, T : IDirectional {
    fun againstWall() = isBehaviorActive(BehaviorType.WALL_SLIDING) ||
        (body.isSensingAny(BodySense.SIDE_TOUCHING_BLOCK_LEFT, BodySense.SIDE_TOUCHING_BLOCK_RIGHT) &&
            !body.isSensing(BodySense.FEET_ON_GROUND))

    fun upSpeed() = ConstVals.PPM * when {
        params.wallJumpAllowed() -> params.wallJumpSpeed()
        else -> params.jumpSpeed()
    }

    return FunctionalBehaviorImpl(
        evaluate = {
            if (!existenceState.liveAfterUpdate || !inPlay || !playerCanMove || damaged || teleporting ||
                isAnyBehaviorActive(BehaviorType.SWIMMING, BehaviorType.CLIMBING, BehaviorType.JETPACKING) ||
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) ||
                (!input.isPressed(MegaControllerButton.A) && !params.ignoreRelease()) ||
                (bodyOverGround && input.isPressed(MegaControllerButton.DOWN)) ||
                params.blocked()
            ) return@FunctionalBehaviorImpl false

            if (isBehaviorActive(BehaviorType.JUMPING)) {
                val velocity = body.physics.velocity
                return@FunctionalBehaviorImpl when (direction) {
                    Direction.UP -> velocity.y > 0f
                    Direction.DOWN -> velocity.y < 0f
                    Direction.LEFT -> velocity.x < 0f
                    Direction.RIGHT -> velocity.x > 0f
                }
            }

            return@FunctionalBehaviorImpl params.canStart() &&
                input.isJustPressed(MegaControllerButton.A) &&
                (bodyOverGround || params.wallJumpAllowed())
        },
        init = {
            when {
                direction.isVertical() -> body.physics.frictionOnSelf.y = 1f
                else -> body.physics.frictionOnSelf.x = 1f
            }

            val wallKick = params.wallJumpHorizontal * ConstVals.PPM * facing.value

            val v = GameObjectPools.fetch(Vector2::class)
            v.x = when (direction) {
                Direction.UP -> if (againstWall()) wallKick else body.physics.velocity.x
                Direction.DOWN -> if (againstWall()) -wallKick else body.physics.velocity.x
                Direction.LEFT, Direction.RIGHT -> upSpeed()
            }
            v.y = when (direction) {
                Direction.UP, Direction.DOWN -> upSpeed()
                Direction.LEFT -> if (againstWall()) wallKick else body.physics.velocity.y
                Direction.RIGHT -> if (againstWall()) -wallKick else body.physics.velocity.y
            }
            body.physics.velocity.let {
                when {
                    body.isSensingAll(BodySense.IN_WATER, BodySense.FORCE_APPLIED) -> it.add(v)
                    else -> it.set(v)
                }
            }

            params.onJump()

            stopMomentum(false)

            GameLogger.debug(TAG, "init(): velocity=$v")
        },
        end = {
            when {
                direction.isVertical() -> body.physics.velocity.y = 0f
                else -> body.physics.velocity.x = 0f
            }

            GameLogger.debug(TAG, "end(): velocity=${body.physics.velocity}")
        })
}
