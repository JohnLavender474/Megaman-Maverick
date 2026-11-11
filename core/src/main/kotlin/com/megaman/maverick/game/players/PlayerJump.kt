package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.behaviors.FunctionalBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
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
    val startPressed: () -> Boolean,
    val downBlocksGroundJump: Boolean,
    val endKeepsFall: Boolean,
    val onJump: () -> Unit
)

// TODO: widen the player type when an enemy clone calls this (item 121)
fun playerJumpBehavior(player: MegaPlayer, params: JumpParams): IBehavior {
    fun againstWall() = player.isBehaviorActive(BehaviorType.WALL_SLIDING) ||
        (player.body.isSensingAny(BodySense.SIDE_TOUCHING_BLOCK_LEFT, BodySense.SIDE_TOUCHING_BLOCK_RIGHT) &&
            !player.body.isSensing(BodySense.FEET_ON_GROUND))

    fun upSpeed() = ConstVals.PPM * when {
        params.wallJumpAllowed() -> params.wallJumpSpeed()
        else -> params.jumpSpeed()
    }

    return FunctionalBehaviorImpl(
        evaluate = {
            if (!player.existenceState.liveAfterUpdate || !player.inPlay || !player.playerCanMove || player.damaged || player.teleporting ||
                player.isAnyBehaviorActive(BehaviorType.SWIMMING, BehaviorType.CLIMBING, BehaviorType.JETPACKING) ||
                player.body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) ||
                (!player.input.isPressed(MegaControllerButton.A) && !params.ignoreRelease()) ||
                (params.downBlocksGroundJump && player.bodyOverGround && player.input.isPressed(MegaControllerButton.DOWN)) ||
                params.blocked()
            ) return@FunctionalBehaviorImpl false

            if (player.isBehaviorActive(BehaviorType.JUMPING)) {
                val velocity = player.body.physics.velocity
                return@FunctionalBehaviorImpl when (player.direction) {
                    Direction.UP -> velocity.y > 0f
                    Direction.DOWN -> velocity.y < 0f
                    Direction.LEFT -> velocity.x < 0f
                    Direction.RIGHT -> velocity.x > 0f
                }
            }

            return@FunctionalBehaviorImpl params.canStart() &&
                params.startPressed() &&
                (player.bodyOverGround || params.wallJumpAllowed())
        },
        init = {
            when {
                player.direction.isVertical() -> player.body.physics.frictionOnSelf.y = 1f
                else -> player.body.physics.frictionOnSelf.x = 1f
            }

            val wallKick = params.wallJumpHorizontal * ConstVals.PPM * player.facing.value

            val v = GameObjectPools.fetch(Vector2::class)
            v.x = when (player.direction) {
                Direction.UP -> if (againstWall()) wallKick else player.body.physics.velocity.x
                Direction.DOWN -> if (againstWall()) -wallKick else player.body.physics.velocity.x
                Direction.LEFT, Direction.RIGHT -> upSpeed()
            }
            v.y = when (player.direction) {
                Direction.UP, Direction.DOWN -> upSpeed()
                Direction.LEFT -> if (againstWall()) wallKick else player.body.physics.velocity.y
                Direction.RIGHT -> if (againstWall()) -wallKick else player.body.physics.velocity.y
            }
            player.body.physics.velocity.let {
                when {
                    player.body.isSensingAll(BodySense.IN_WATER, BodySense.FORCE_APPLIED) -> it.add(v)
                    else -> it.set(v)
                }
            }

            params.onJump()

            player.stopMomentum(false)

            GameLogger.debug(TAG, "init(): velocity=$v")
        },
        end = {
            val velocity = player.body.physics.velocity
            fun stop(speed: Float, fallSign: Float) =
                if (params.endKeepsFall && speed * fallSign > 0f) speed else 0f
            when (player.direction) {
                Direction.UP -> velocity.y = stop(velocity.y, -1f)
                Direction.DOWN -> velocity.y = stop(velocity.y, 1f)
                Direction.LEFT -> velocity.x = stop(velocity.x, 1f)
                Direction.RIGHT -> velocity.x = stop(velocity.x, -1f)
            }

            GameLogger.debug(TAG, "end(): velocity=${player.body.physics.velocity}")
        })
}
