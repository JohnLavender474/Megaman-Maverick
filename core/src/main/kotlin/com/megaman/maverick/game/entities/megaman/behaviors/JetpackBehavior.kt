package com.megaman.maverick.game.entities.megaman.behaviors

import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.input
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.teleporting

internal fun Megaman.jetpackBehavior(): IBehavior {
    return object : AbstractBehaviorImpl() {

        override fun evaluate(delta: Float): Boolean {
            if (!existenceState.liveAfterUpdate || !inPlay || !canMove || damaged || teleporting || currentWeapon != MegamanWeapon.RUSH_JET ||
                !input.isPressed(MegaControllerButton.A) || !input.isPressed(MegaControllerButton.UP) ||
                isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.AIR_DASHING, BehaviorType.GROUND_SLIDING) ||
                weaponsHandler.isDepleted(MegamanWeapon.RUSH_JET) ||
                game.camera.rotating
            ) return false

            return if (isBehaviorActive(BehaviorType.JETPACKING))
                input.isPressed(MegaControllerButton.A) && input.isPressed(MegaControllerButton.UP)
            else input.getStatus(MegaControllerButton.UP) == ButtonStatus.PRESSED &&
                input.getStatus(MegaControllerButton.A) == ButtonStatus.JUST_PRESSED
        }

        override fun init(vararg params: Any) {
            body.physics.gravityOn = false
        }

        override fun act(delta: Float) {
            body.physics.gravityOn = false

            val impulse = MegamanValues.JETPACK_Y_IMPULSE * ConstVals.PPM * movementScalar * delta

            when (direction) {
                Direction.UP -> {
                    if (body.physics.velocity.y < 0f) body.physics.velocity.y = 0f
                    body.physics.velocity.y += impulse
                }

                Direction.DOWN -> {
                    if (body.physics.velocity.y > 0f) body.physics.velocity.y = 0f
                    body.physics.velocity.y += -impulse
                }

                Direction.LEFT -> {
                    if (body.physics.velocity.x > 0f) body.physics.velocity.x = 0f
                    body.physics.velocity.x += -impulse
                }

                Direction.RIGHT -> {
                    if (body.physics.velocity.x < 0f) body.physics.velocity.x = 0f
                    body.physics.velocity.x += impulse
                }
            }
        }

        override fun end() {
            jetpackHandler.reset()
            if (!game.camera.rotating) body.physics.gravityOn = true
        }
    }
}
