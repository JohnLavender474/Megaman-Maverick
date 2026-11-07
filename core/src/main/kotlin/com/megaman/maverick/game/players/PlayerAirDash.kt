package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.players.playerSlot
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
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

private fun AbstractHealthEntity.airDashButton() = when (services.players.settings(playerSlot!!).selectButtonAction) {
    SelectButtonAction.AIR_DASH -> MegaControllerButton.SELECT
    else -> MegaControllerButton.A
}

fun AbstractHealthEntity.airDashInputPressed() = input.isPressed(airDashButton())

fun AbstractHealthEntity.airDashInputJustPressed() = input.isJustPressed(airDashButton())

fun <T> T.playerAirDashBehavior(params: AirDashParams): IBehavior
    where T : AbstractHealthEntity, T : IBodyEntity, T : IFaceable, T : IDirectional {
    val entity = this
    return object : AbstractBehaviorImpl() {

        private val minTimer = Timer(params.minTime)
        private val maxTimer = Timer(Float.MAX_VALUE)
        private val impulse = Vector2()
        private var lastFacing = Facing.RIGHT
        private var time = 0f
        private var decel = 0f

        override fun evaluate(delta: Float): Boolean {
            if (!existenceState.liveAfterUpdate || !inPlay || !playerCanMove || damaged || teleporting ||
                maxTimer.isFinished() ||
                isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.JETPACKING, BehaviorType.WALL_SLIDING) ||
                FacingUtils.isFacingBlock(entity) || params.blocked()
            ) return false

            if (isBehaviorActive(BehaviorType.AIR_DASHING)) return !minTimer.isFinished() || params.held()

            return params.canStart()
        }

        override fun init(vararg args: Any) {
            GameLogger.debug(TAG, "init()")

            minTimer.reset()
            maxTimer.resetDuration(params.maxTime())
            time = 0f

            body.physics.gravityOn = false

            params.onStart()

            if (direction.isVertical()) impulse.y = 0f else impulse.x = 0f

            decel = params.decel()

            val impulseValue = ConstVals.PPM * params.speed()
            when (direction) {
                Direction.UP -> impulse.x = impulseValue * facing.value
                Direction.DOWN -> impulse.x = impulseValue * -facing.value
                Direction.LEFT -> impulse.y = impulseValue * facing.value
                Direction.RIGHT -> impulse.y = impulseValue * -facing.value
            }
            lastFacing = facing
        }

        override fun act(delta: Float) {
            minTimer.update(delta)
            maxTimer.update(delta)
            time += delta

            if (time < params.startDelay) {
                body.physics.velocity.setZero()
                return
            }

            if (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)
            ) return

            if (facing != lastFacing) impulse.scl(-1f)
            lastFacing = facing

            if (decel > 0f) impulse.setLength(
                max(params.minSpeed * ConstVals.PPM, impulse.len() - decel * ConstVals.PPM * delta)
            )

            body.physics.velocity.set(impulse)

            preserveMomentum()
        }

        override fun end() {
            GameLogger.debug(TAG, "end()")

            minTimer.reset()
            maxTimer.reset()

            if (!services.gameCamera.rotating) {
                body.physics.gravityOn = true

                if (!teleporting) {
                    val impulseOnEnd = facing.value * ConstVals.PPM * params.endBump()
                    when (direction) {
                        Direction.UP -> body.physics.velocity.x += impulseOnEnd
                        Direction.DOWN -> body.physics.velocity.x -= impulseOnEnd
                        Direction.LEFT -> body.physics.velocity.y += impulseOnEnd
                        Direction.RIGHT -> body.physics.velocity.y -= impulseOnEnd
                    }
                }
            }

            params.onEnd()

            preserveMomentum()

            requestMomentumKill()
        }
    }
}
