package com.megaman.maverick.game.entities.megaman.behaviors

import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.*

internal fun Megaman.jetpackBehavior(): IBehavior {
    return object : AbstractBehaviorImpl() {

        override fun evaluate(delta: Float): Boolean {
            if (dead || !ready || !canMove || damaged || teleporting || currentWeapon != MegamanWeapon.RUSH_JET ||
                !game.controllerPoller.areAllPressed(gdxArrayOf(MegaControllerButton.A, MegaControllerButton.UP)) ||
                isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.AIR_DASHING, BehaviorType.GROUND_SLIDING) ||
                weaponsHandler.isDepleted(MegamanWeapon.RUSH_JET) ||
                game.isCameraRotating()
            ) return false

            return if (isBehaviorActive(BehaviorType.JETPACKING)) game.controllerPoller.areAllPressed(
                gdxArrayOf(MegaControllerButton.A, MegaControllerButton.UP)
            ) else game.controllerPoller.allMatch(
                objectMapOf(
                    MegaControllerButton.UP pairTo ButtonStatus.PRESSED,
                    MegaControllerButton.A pairTo ButtonStatus.JUST_PRESSED
                )
            )
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
            if (!game.isCameraRotating()) body.physics.gravityOn = true
        }
    }
}
