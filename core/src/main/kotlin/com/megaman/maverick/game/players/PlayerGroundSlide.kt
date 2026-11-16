package com.megaman.maverick.game.players

import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.killPostActionMomentum
import com.megaman.maverick.game.entities.state.preserveMomentum
import com.megaman.maverick.game.entities.state.requestMomentumKill
import com.megaman.maverick.game.entities.state.shouldPreserveMomentum
import com.megaman.maverick.game.entities.state.stopMomentum
import com.megaman.maverick.game.world.body.*

private const val TAG = "PlayerGroundSlide"

class GroundSlideParams(
    val minTime: Float,
    val maxTime: Float,
    val cooldown: Float,
    val speed: () -> Float,
    val blocked: () -> Boolean
)

// TODO: widen the player type when an enemy clone calls this (item 121)
fun playerGroundSlideBehavior(player: MegaPlayer, params: GroundSlideParams): IBehavior =
    object : AbstractBehaviorImpl() {

        private val minTimer = Timer(params.minTime)
        private val maxTimer = Timer(params.maxTime)
        private val cooldown = Timer(params.cooldown)

        private var directionOnInit: Direction? = null

        override fun evaluate(delta: Float): Boolean {
            cooldown.update(delta)

            val body = player.body
            if (!player.existenceState.liveAfterUpdate || !player.inPlay || !player.playerCanMove ||
                player.services.gameCamera.rotating || body.isSensing(BodySense.FEET_ON_SAND) ||
                player.isBehaviorActive(BehaviorType.JETPACKING) || !cooldown.isFinished() || params.blocked()
            ) return false

            if (player.bodyOverGround &&
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) &&
                player.isBehaviorActive(BehaviorType.GROUND_SLIDING)
            ) return true

            if (!player.bodyOverGround) return false

            if (player.damaged || maxTimer.isFinished() ||
                (minTimer.isFinished() && !player.input.isPressed(MegaControllerButton.DOWN))
            ) return false

            return when {
                player.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> !minTimer.isFinished() ||
                    (player.input.isPressed(MegaControllerButton.A) && directionOnInit == player.direction)
                else -> body.isSensing(BodySense.FEET_ON_GROUND) &&
                    player.input.isPressed(MegaControllerButton.DOWN) &&
                    player.input.isJustPressed(MegaControllerButton.A)
            }
        }

        override fun init(vararg params: Any) {
            GameLogger.debug(TAG, "init()")
            minTimer.reset()
            maxTimer.reset()

            val body = player.body
            when (player.direction) {
                Direction.UP -> {}
                Direction.DOWN -> body.translate(0f, 0.6f * ConstVals.PPM)
                Direction.LEFT -> body.translate(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM * player.facing.value)
                Direction.RIGHT -> body.translate(-0.5f * ConstVals.PPM, -0.1f * ConstVals.PPM * player.facing.value)
            }

            directionOnInit = player.direction
        }

        override fun act(delta: Float) {
            minTimer.update(delta)
            maxTimer.update(delta)
            if (player.damaged) return

            val speed = params.speed() * ConstVals.PPM
            val velocity = player.body.physics.velocity
            when (player.direction) {
                Direction.UP -> velocity.x = speed * player.facing.value
                Direction.DOWN -> velocity.x = speed * -player.facing.value
                Direction.LEFT -> velocity.y = speed * player.facing.value
                Direction.RIGHT -> velocity.y = speed * -player.facing.value
            }

            if (player.shouldPreserveMomentum(false)) player.preserveMomentum() else player.stopMomentum(false)
        }

        override fun end() {
            GameLogger.debug(TAG, "end()")
            if (player.direction.isHorizontal()) player.body.translate(0f, -0.35f * ConstVals.PPM)
            minTimer.reset()
            maxTimer.reset()
            cooldown.reset()
            if (player.shouldPreserveMomentum(false)) player.preserveMomentum() else player.stopMomentum(true)
            if (player.bodyOverGround) player.killPostActionMomentum() else player.requestMomentumKill()
        }
    }
