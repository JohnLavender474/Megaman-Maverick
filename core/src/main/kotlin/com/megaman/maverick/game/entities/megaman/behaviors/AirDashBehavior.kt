package com.megaman.maverick.game.entities.megaman.behaviors

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.time.Timer
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.bodyOverGround
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.input
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

const val MEGAMAN_AIR_DASH_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: AirDashBehavior"

internal fun Megaman.airDashBehavior(): IBehavior {
    return object : AbstractBehaviorImpl() {

        private val minTimer = Timer(MegamanValues.AIR_DASH_MIN_TIME)
        private val maxTimer = Timer(MegamanValues.AIR_DASH_MAX_TIME)
        private val impulse = Vector2()
        private var lastFacing = Facing.RIGHT

        private fun isAirDashButtonActivated() = when (game.selectButtonAction) {
            SelectButtonAction.AIR_DASH -> input.isPressed(MegaControllerButton.SELECT)
            else -> input.isPressed(MegaControllerButton.A)
        }

        private fun isAirDashButtonJustActivated() = when (game.selectButtonAction) {
            SelectButtonAction.AIR_DASH -> input.isJustPressed(MegaControllerButton.SELECT)
            else -> input.isJustPressed(MegaControllerButton.A)
        }

        override fun evaluate(delta: Float): Boolean {
            if (dead || !ready || !canMove || damaged || teleporting || maxTimer.isFinished() ||
                isAnyBehaviorActive(
                    BehaviorType.CLIMBING,
                    BehaviorType.JETPACKING,
                    BehaviorType.WALL_SLIDING,
                ) || FacingUtils.isFacingBlock(this@airDashBehavior) ||
                (currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting) ||
                (currentWeapon != MegamanWeapon.RUSH_JET && bodyOverGround) ||
                (currentWeapon == MegamanWeapon.RUSH_JET &&
                    (weaponsHandler.isDepleted(MegamanWeapon.RUSH_JET) ||
                        input.isPressed(MegaControllerButton.UP)))
            ) return false

            if (isBehaviorActive(BehaviorType.AIR_DASHING))
                return !minTimer.isFinished() || isAirDashButtonActivated()

            return aButtonTask == AButtonTask.AIR_DASH && isAirDashButtonJustActivated()
        }

        override fun init(vararg params: Any) {
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "init()")

            minTimer.reset()
            maxTimer.resetDuration(
                if (currentWeapon == MegamanWeapon.RUSH_JET) MegamanValues.RUSH_JET_AIR_DASH_MAX_TIME
                else MegamanValues.AIR_DASH_MAX_TIME
            )

            body.physics.gravityOn = false

            aButtonTask = AButtonTask.JUMP
            airDashConsumed = true

            if (direction.isVertical()) impulse.y = 0f else impulse.x = 0f

            var impulseValue = ConstVals.PPM * movementScalar *
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_VEL else MegamanValues.AIR_DASH_VEL)
            if (currentWeapon == MegamanWeapon.RUSH_JET) impulseValue *= MegamanValues.RUSH_JET_AIR_DASH_SCALAR

            when (direction) {
                Direction.UP -> impulse.x = impulseValue * facing.value
                Direction.DOWN -> impulse.x = impulseValue * -facing.value
                Direction.LEFT -> impulse.y = impulseValue * facing.value
                Direction.RIGHT -> impulse.y = impulseValue * -facing.value
            }
            lastFacing = facing
            putProperty(MegamanKeys.DIRECTION_ON_AIR_DASH, direction)
        }

        override fun act(delta: Float) {
            minTimer.update(delta)
            maxTimer.update(delta)

            if (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)
            ) return

            if (facing != lastFacing) impulse.scl(-1f)
            lastFacing = facing

            body.physics.velocity.set(impulse)

            // Always preserve momentum when air dashing
            preserveMomentum()
        }

        override fun end() {
            GameLogger.debug(MEGAMAN_AIR_DASH_BEHAVIOR_TAG, "end()")

            minTimer.reset()
            maxTimer.reset()

            if (!game.isCameraRotating()) {
                body.physics.gravityOn = true

                if (!teleporting) {
                    val impulseOnEnd =
                        facing.value * ConstVals.PPM * if (body.isSensing(BodySense.IN_WATER))
                            MegamanValues.WATER_AIR_DASH_END_BUMP else MegamanValues.AIR_DASH_END_BUMP

                    when (direction) {
                        Direction.UP -> body.physics.velocity.x += impulseOnEnd
                        Direction.DOWN -> body.physics.velocity.x -= impulseOnEnd
                        Direction.LEFT -> body.physics.velocity.y += impulseOnEnd
                        Direction.RIGHT -> body.physics.velocity.y -= impulseOnEnd
                    }
                }
            }

            if (currentWeapon == MegamanWeapon.RUSH_JET) jetpackHandler.reset()

            // Always preserve momentum when air dashing
            preserveMomentum()

            // Kill excess horizontal momentum on landing after the dash so players
            // can perform tight platforming without sliding past their target.
            pendingPostActionMomentumKill = true
        }
    }
}
