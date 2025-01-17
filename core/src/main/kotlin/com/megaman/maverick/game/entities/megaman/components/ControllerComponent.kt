package com.megaman.maverick.game.entities.megaman.components

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.controller.ControllerComponent
import com.mega.game.engine.controller.buttons.ButtonActuator
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.extensions.shoot
import com.megaman.maverick.game.entities.megaman.extensions.stopCharging
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs

const val MEGAMAN_CONTROLLER_COMPONENT_TAG = "MegamanControllerComponent"

internal fun Megaman.defineControllerComponent(): ControllerComponent {
    val left = ButtonActuator(
        onJustPressed = { _ ->
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "left actuator just pressed")
        },
        onPressContinued = { poller, delta ->
            if (!canMove || !ready || damaged || poller.isPressed(MegaControllerButton.RIGHT) || teleporting ||
                isBehaviorActive(BehaviorType.GROUND_SLIDING)
            ) {
                if (!poller.isPressed(MegaControllerButton.RIGHT)) running = false
                return@ButtonActuator
            }

            facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.RIGHT else Facing.LEFT
            if (direction.equalsAny(Direction.DOWN, Direction.RIGHT)) swapFacing()

            if (isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.RIDING_CART)) return@ButtonActuator
            running = !isBehaviorActive(BehaviorType.WALL_SLIDING)

            val threshold =
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_RUN_SPEED
                else MegamanValues.RUN_SPEED) * ConstVals.PPM

            val rawImpulse =
                if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
                else MegamanValues.RUN_IMPULSE

            val impulse = rawImpulse * delta * movementScalar * ConstVals.PPM * facing.value *
                if (isBehaviorActive(BehaviorType.WALL_SLIDING)) -1f else 1f

            if (direction.isVertical() && abs(body.physics.velocity.x) < threshold)
                body.physics.velocity.x += impulse
            else if (direction.isHorizontal() && abs(body.physics.velocity.y) < threshold)
                body.physics.velocity.y += impulse
        },
        onJustReleased = { poller ->
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "left actuator just released")
            if (!poller.isPressed(MegaControllerButton.RIGHT)) running = false
        },
        onReleaseContinued = { poller, _ ->
            if (!poller.isPressed(MegaControllerButton.RIGHT)) running = false
        })

    val right = ButtonActuator(
        onJustPressed = { _ ->
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "right actuator just pressed")
        },
        onPressContinued = { poller, delta ->
            if (!canMove || !ready || damaged || poller.isPressed(MegaControllerButton.LEFT) || teleporting ||
                isBehaviorActive(BehaviorType.GROUND_SLIDING)
            ) {
                if (!poller.isPressed(MegaControllerButton.LEFT)) running = false
                return@ButtonActuator
            }

            facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.LEFT else Facing.RIGHT
            if (direction.equalsAny(Direction.DOWN, Direction.RIGHT)) swapFacing()

            if (isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.RIDING_CART)) return@ButtonActuator
            running = !isBehaviorActive(BehaviorType.WALL_SLIDING)

            val threshold =
                (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_RUN_SPEED
                else MegamanValues.RUN_SPEED) * ConstVals.PPM

            val rawImpulse =
                if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
                else MegamanValues.RUN_IMPULSE

            val impulse = rawImpulse * delta * movementScalar * ConstVals.PPM * facing.value *
                if (isBehaviorActive(BehaviorType.WALL_SLIDING)) -1f else 1f

            if (direction.isVertical() && abs(body.physics.velocity.x) < threshold)
                body.physics.velocity.x += impulse
            else if (direction.isHorizontal() && abs(body.physics.velocity.y) < threshold)
                body.physics.velocity.y += impulse
        },
        onJustReleased = { poller ->
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "right actuator just released")
            if (!poller.isPressed(MegaControllerButton.LEFT)) running = false
        },
        onReleaseContinued = { poller, _ ->
            if (!poller.isPressed(MegaControllerButton.LEFT)) running = false
        }
    )

    val attack = ButtonActuator(
        onPressContinued = { _, delta ->
            if (!ready || stunned || damaged /* TODO: || game.isCameraRotating() */ || teleporting ||
                currentWeapon == MegamanWeapon.RUSH_JETPACK ||
                (!charging && !weaponHandler.canFireWeapon(currentWeapon, MegaChargeStatus.HALF_CHARGED)) ||
                (charging && !weaponHandler.canFireWeapon(currentWeapon, MegaChargeStatus.FULLY_CHARGED) ||
                    !has(MegaAbility.CHARGE_WEAPONS))
            ) {
                stopCharging()
                return@ButtonActuator
            }

            val scalar = when {
                has(MegaEnhancement.FASTER_BUSTER_CHARGING) -> MegaEnhancement.FASTER_BUSTER_CHARGING_SCALAR
                else -> 1f
            }
            chargingTimer.update(delta * scalar)
        },
        onJustReleased = {
            if (stunned || damaged || game.isCameraRotating() || teleporting || !ready ||
                !weaponHandler.canFireWeapon(currentWeapon, chargeStatus) ||
                game.isProperty(ConstKeys.ROOM_TRANSITION, true)
            ) {
                stopCharging()
                return@ButtonActuator
            }

            shoot()
            stopCharging()
        },
    )

    val changeWeapon = ButtonActuator(onJustReleased = { _ ->
        if (!ready || damaged || teleporting) return@ButtonActuator
        setToNextWeapon()
    })

    return ControllerComponent(
        MegaControllerButton.LEFT pairTo { left },
        MegaControllerButton.RIGHT pairTo { right },
        MegaControllerButton.B pairTo { attack },
        MegaControllerButton.SELECT pairTo { changeWeapon })
}
