package com.megaman.maverick.game.entities.megaman.behaviors

import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.time.Timer
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.bodyOverGround
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.input
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.world.body.*

const val MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: GroundSlideBehavior"

internal fun Megaman.groundSlideBehavior(): IBehavior {
    return object : AbstractBehaviorImpl() {

        private val minTimer = Timer(MegamanValues.GROUND_SLIDE_MIN_TIME)
        private val maxTimer = Timer(MegamanValues.GROUND_SLIDE_MAX_TIME)
        private val cooldown = Timer(MegamanValues.GROUND_SLIDE_COOLDOWN)

        private var directionOnInit: Direction? = null

        override fun evaluate(delta: Float): Boolean {
            cooldown.update(delta)

            if (!existenceState.liveAfterUpdate || !inPlay || !canMove || game.camera.rotating || body.isSensing(BodySense.FEET_ON_SAND) ||
                isBehaviorActive(BehaviorType.JETPACKING) || !cooldown.isFinished() ||
                (currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting)
            ) return false

            if (bodyOverGround &&
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) &&
                isBehaviorActive(BehaviorType.GROUND_SLIDING)
            ) return true

            if (!bodyOverGround) return false

            if (damaged || maxTimer.isFinished() ||
                (minTimer.isFinished() && !input.isPressed(MegaControllerButton.DOWN))
            ) return false

            return when {
                isBehaviorActive(BehaviorType.GROUND_SLIDING) -> !minTimer.isFinished() ||
                    (input.isPressed(MegaControllerButton.A) && directionOnInit == direction)
                else -> body.isSensing(BodySense.FEET_ON_GROUND) &&
                    input.isPressed(MegaControllerButton.DOWN) &&
                    input.isJustPressed(MegaControllerButton.A)
            }
        }

        override fun init(vararg params: Any) {
            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "init()")

            minTimer.reset()
            maxTimer.reset()

            when (direction) {
                Direction.UP -> {}
                Direction.DOWN -> body.translate(0f, 0.6f * ConstVals.PPM)
                Direction.LEFT -> body.translate(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM * facing.value)
                Direction.RIGHT -> body.translate(-0.5f * ConstVals.PPM, -0.1f * ConstVals.PPM * facing.value)
            }

            directionOnInit = direction
        }

        override fun act(delta: Float) {
            minTimer.update(delta)
            maxTimer.update(delta)

            val facingBlockLeft = (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT))
            val facingBlockRight = (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))

            if (damaged) {
                GameLogger.debug(
                    MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "blocked from act: " +
                        "damaged=$damaged, " +
                        "facingBlockLeft=$facingBlockLeft, " +
                        "facingBlockRight=$facingBlockRight"
                )
                return
            }

            var vel = (when {
                body.isSensing(BodySense.IN_WATER) -> MegamanValues.WATER_GROUND_SLIDE_VEL
                else -> MegamanValues.GROUND_SLIDE_VEL
            }) * ConstVals.PPM * movementScalar

            if (game.state.containsEnhancement(MegaEnhancement.GROUND_SLIDE_BOOST)) vel *= MegaEnhancement.GROUND_SLIDE_BOOST_SCALAR

            when (direction) {
                Direction.UP -> body.physics.velocity.x = vel * facing.value
                Direction.DOWN -> body.physics.velocity.x = vel * -facing.value
                Direction.LEFT -> body.physics.velocity.y = vel * facing.value
                Direction.RIGHT -> body.physics.velocity.y = vel * -facing.value
            }

            if (shouldPreserveMomentum(false)) preserveMomentum() else stopMomentum(false)
        }

        override fun end() {
            GameLogger.debug(MEGAMAN_GROUND_SLIDE_BEHAVIOR_TAG, "end()")

            if (direction.isHorizontal()) body.translate(0f, -0.35f * ConstVals.PPM)

            minTimer.reset()
            maxTimer.reset()
            cooldown.reset()

            if (shouldPreserveMomentum(false)) preserveMomentum() else stopMomentum(true)

            // Clamp horizontal velocity to max run speed and trigger the friction
            // burst so post-slide overspeed bleeds off quickly on the ground.
            if (bodyOverGround) killPostActionMomentum() else pendingPostActionMomentumKill = true
        }
    }
}
