package com.megaman.maverick.game.entities.megaman.behaviors

import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Direction
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.input
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

internal fun Megaman.crouchBehavior(): IBehavior {
    return object : AbstractBehaviorImpl() {

        override fun evaluate(delta: Float): Boolean {
            when {
                !input.isPressed(MegaControllerButton.DOWN) -> buttonToCrouchDelay.reset()
                else -> buttonToCrouchDelay.update(delta)
            }

            when {
                isBehaviorActive(BehaviorType.GROUND_SLIDING) -> groundSlideToCrouchDelay.reset()
                else -> groundSlideToCrouchDelay.update(delta)
            }

            if (!buttonToCrouchDelay.isFinished() || !groundSlideToCrouchDelay.isFinished()) return false

            if (!existenceState.liveAfterUpdate || damaged || !inPlay || !canMove || services.camera.rotating ||
                isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.JETPACKING) ||
                !body.isSensing(BodySense.FEET_ON_GROUND) || body.isSensing(BodySense.FEET_ON_SAND) ||
                !input.isPressed(MegaControllerButton.DOWN) ||
                (currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting) ||
                when {
                    direction.isVertical() -> abs(body.physics.velocity.x)
                    else -> abs(body.physics.velocity.y)
                } > MegamanValues.CROUCH_MAX_VEL * ConstVals.PPM
            ) return false

            return true
        }

        override fun init(vararg params: Any) {
            if (direction.isVertical()) body.physics.velocity.x = 0f else body.physics.velocity.y = 0f
            when (direction) {
                Direction.UP -> {}
                Direction.DOWN -> body.translate(0f, 0.75f * ConstVals.PPM)
                Direction.LEFT -> body.translate(0.5f * ConstVals.PPM, 0.3f * ConstVals.PPM)
                Direction.RIGHT -> body.translate(-0.5f * ConstVals.PPM, 0.3f * ConstVals.PPM)
            }
        }

        override fun end() {
            if (!isBehaviorActive(BehaviorType.GROUND_SLIDING) && direction.isHorizontal())
                body.translate(0f, -0.3f * ConstVals.PPM)
        }
    }
}
