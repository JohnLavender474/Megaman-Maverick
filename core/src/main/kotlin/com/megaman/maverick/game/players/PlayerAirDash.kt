package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.preserveMomentum
import com.megaman.maverick.game.entities.state.requestMomentumKill
import com.megaman.maverick.game.entities.state.teleporting
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.max

private const val TAG = "PlayerAirDash"

class AirDashParams(
    val blocked: () -> Boolean,
    val canStart: () -> Boolean,
    val held: () -> Boolean,
    val startDelay: Float,
    val minTime: Float,
    val maxTime: () -> Float,
    val speed: () -> Float,
    val decel: () -> Float,
    val minSpeed: Float,
    val endBump: () -> Float,
    val onStart: () -> Unit,
    val onEnd: () -> Unit
)

private fun airDashButton(player: MegaPlayer) = when (player.services.players.settings(player.slot).selectButtonAction) {
    SelectButtonAction.AIR_DASH -> MegaControllerButton.SELECT
    else -> MegaControllerButton.A
}

// TODO: widen the player type when an enemy clone calls this (item 121)
fun airDashInputPressed(player: MegaPlayer) = player.input.isPressed(airDashButton(player))

// TODO: widen the player type when an enemy clone calls this (item 121)
fun airDashInputJustPressed(player: MegaPlayer) = player.input.isJustPressed(airDashButton(player))

// TODO: widen the player type when an enemy clone calls this (item 121)
fun playerAirDashBehavior(player: MegaPlayer, params: AirDashParams): IBehavior {
    return object : AbstractBehaviorImpl() {

        private val minTimer = Timer(params.minTime)
        private val maxTimer = Timer(Float.MAX_VALUE)
        private val impulse = Vector2()
        private var lastFacing = Facing.RIGHT
        private var time = 0f
        private var decel = 0f

        override fun evaluate(delta: Float): Boolean {
            if (!player.existenceState.liveAfterUpdate || !player.inPlay || !player.playerCanMove || player.damaged || player.teleporting ||
                maxTimer.isFinished() ||
                player.isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.JETPACKING, BehaviorType.WALL_SLIDING) ||
                FacingUtils.isFacingBlock(player) || params.blocked()
            ) return false

            if (player.isBehaviorActive(BehaviorType.AIR_DASHING)) return !minTimer.isFinished() || params.held()

            return params.canStart()
        }

        override fun init(vararg args: Any) {
            GameLogger.debug(TAG, "init()")

            minTimer.reset()
            maxTimer.resetDuration(params.maxTime())
            time = 0f

            player.body.physics.gravityOn = false

            params.onStart()

            if (player.direction.isVertical()) impulse.y = 0f else impulse.x = 0f

            decel = params.decel()

            val impulseValue = ConstVals.PPM * params.speed()
            when (player.direction) {
                Direction.UP -> impulse.x = impulseValue * player.facing.value
                Direction.DOWN -> impulse.x = impulseValue * -player.facing.value
                Direction.LEFT -> impulse.y = impulseValue * player.facing.value
                Direction.RIGHT -> impulse.y = impulseValue * -player.facing.value
            }
            lastFacing = player.facing
        }

        override fun act(delta: Float) {
            minTimer.update(delta)
            maxTimer.update(delta)
            time += delta

            if (time < params.startDelay) {
                player.body.physics.velocity.setZero()
                return
            }

            if (player.isFacing(Facing.LEFT) && player.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                player.isFacing(Facing.RIGHT) && player.body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)
            ) return

            if (player.facing != lastFacing) impulse.scl(-1f)
            lastFacing = player.facing

            if (decel > 0f) impulse.setLength(
                max(params.minSpeed * ConstVals.PPM, impulse.len() - decel * ConstVals.PPM * delta)
            )

            player.body.physics.velocity.set(impulse)

            player.preserveMomentum()
        }

        override fun end() {
            GameLogger.debug(TAG, "end()")

            minTimer.reset()
            maxTimer.reset()

            if (!player.services.gameCamera.rotating) {
                player.body.physics.gravityOn = true

                if (!player.teleporting) {
                    val impulseOnEnd = player.facing.value * ConstVals.PPM * params.endBump()
                    when (player.direction) {
                        Direction.UP -> player.body.physics.velocity.x += impulseOnEnd
                        Direction.DOWN -> player.body.physics.velocity.x -= impulseOnEnd
                        Direction.LEFT -> player.body.physics.velocity.y += impulseOnEnd
                        Direction.RIGHT -> player.body.physics.velocity.y -= impulseOnEnd
                    }
                }
            }

            params.onEnd()

            player.preserveMomentum()

            player.requestMomentumKill()
        }
    }
}
