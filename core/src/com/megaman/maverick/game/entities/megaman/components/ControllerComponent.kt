package com.megaman.maverick.game.entities.megaman.components

import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.controller.ControllerComponent
import com.engine.controller.buttons.ButtonActuator
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.ControllerButton
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.extensions.shoot
import com.megaman.maverick.game.entities.megaman.extensions.stopCharging
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing

const val MEGAMAN_CONTROLLER_COMPONENT_TAG = "MegamanControllerComponent"

internal fun Megaman.defineControllerComponent(): ControllerComponent {
    // left
    val left =
        ButtonActuator(
            onJustPressed = { _ ->
                GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "left actuator just pressed")
            },
            onPressContinued = { poller, delta ->
                if (!canMove || !ready || damaged || poller.isPressed(ControllerButton.RIGHT) || teleporting) {
                    if (!poller.isPressed(ControllerButton.RIGHT)) running = false
                    return@ButtonActuator
                }

                facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.RIGHT else Facing.LEFT

                if (isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.RIDING_CART)) return@ButtonActuator
                running = !isBehaviorActive(BehaviorType.WALL_SLIDING)

                val threshold =
                    (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_RUN_SPEED
                    else MegamanValues.RUN_SPEED) * ConstVals.PPM
                val impulse =
                    if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
                    else MegamanValues.RUN_IMPULSE

                if (isDirectionRotatedVertically() && body.physics.velocity.x > -threshold)
                    body.physics.velocity.x -= impulse * delta * ConstVals.PPM
                else if (isDirectionRotatedHorizontally() && body.physics.velocity.y > -threshold)
                    body.physics.velocity.y -= impulse * delta * ConstVals.PPM
            },
            onJustReleased = { poller ->
                GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "left actuator just released")
                if (!poller.isPressed(ControllerButton.RIGHT)) running = false
            },
            onReleaseContinued = { poller, _ ->
                if (!poller.isPressed(ControllerButton.RIGHT)) running = false
            })

    // right
    val right =
        ButtonActuator(
            onJustPressed = { _ ->
                GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "right actuator just pressed")
            },
            onPressContinued = { poller, delta ->
                if (!canMove || !ready || damaged || poller.isPressed(ControllerButton.LEFT) || teleporting) {
                    if (!poller.isPressed(ControllerButton.LEFT)) running = false
                    return@ButtonActuator
                }

                facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.LEFT else Facing.RIGHT

                if (isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.RIDING_CART)) return@ButtonActuator
                running = !isBehaviorActive(BehaviorType.WALL_SLIDING)

                val threshold =
                    (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_RUN_SPEED
                    else MegamanValues.RUN_SPEED) * ConstVals.PPM
                val impulse =
                    if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
                    else MegamanValues.RUN_IMPULSE

                if (isDirectionRotatedVertically() && body.physics.velocity.x < threshold)
                    body.physics.velocity.x += impulse * delta * ConstVals.PPM
                else if (isDirectionRotatedHorizontally() && body.physics.velocity.y < threshold)
                    body.physics.velocity.y += impulse * delta * ConstVals.PPM
            },
            onJustReleased = { poller ->
                GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "right actuator just released")
                if (!poller.isPressed(ControllerButton.LEFT)) running = false
            },
            onReleaseContinued = { poller, _ ->
                if (!poller.isPressed(ControllerButton.LEFT)) running = false
            })

    // attack
    val attack =
        ButtonActuator(
            onPressContinued = { _, delta ->
                if (damaged) {
                    stopCharging()
                    return@ButtonActuator
                }

                if (!charging &&
                    !weaponHandler.canFireWeapon(currentWeapon, MegaChargeStatus.HALF_CHARGED)
                ) {
                    stopCharging()
                    return@ButtonActuator
                }

                if (charging &&
                    !weaponHandler.canFireWeapon(currentWeapon, MegaChargeStatus.FULLY_CHARGED)
                )
                    return@ButtonActuator

                chargingTimer.update(delta)
            },
            onJustReleased = {
                if (damaged) {
                    stopCharging()
                    return@ButtonActuator
                }

                if (!weaponHandler.canFireWeapon(currentWeapon, chargeStatus)) {
                    stopCharging()
                    return@ButtonActuator
                }

                shoot()
                stopCharging()
            },
        )

    // swap weapon
    val select =
        ButtonActuator(
            onJustPressed = {
                // TODO: implement this
                /*
                var x: Int = currWeapon.ordinal() + 1
                if (x >= MegamanWeapon.values().length) {
                    x = 0
                }
                currWeapon = MegamanWeapon.values().get(x)
                 */
            })

    return ControllerComponent(
        this,
        ControllerButton.LEFT to { left },
        ControllerButton.RIGHT to { right },
        ControllerButton.B to { attack },
        ControllerButton.SELECT to { select })
}
