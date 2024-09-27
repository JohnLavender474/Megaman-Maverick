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
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButtons
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaAbility
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
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
            if (!canMove || !ready || damaged || poller.isPressed(MegaControllerButtons.RIGHT) || teleporting) {
                if (!poller.isPressed(MegaControllerButtons.RIGHT)) running = false
                return@ButtonActuator
            }

            facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.RIGHT else Facing.LEFT
            if (directionRotation!!.equalsAny(Direction.DOWN, Direction.RIGHT)) swapFacing()

            if (isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.RIDING_CART)) return@ButtonActuator
            running = !isBehaviorActive(BehaviorType.WALL_SLIDING)

            val threshold = (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_RUN_SPEED
            else MegamanValues.RUN_SPEED) * ConstVals.PPM

            val rawImpulse = if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
            else MegamanValues.RUN_IMPULSE

            val impulse = rawImpulse * delta * movementScalar * ConstVals.PPM * facing.value *
                    if (isBehaviorActive(BehaviorType.WALL_SLIDING)) -1f else 1f

            if (isDirectionRotatedVertically() && abs(body.physics.velocity.x) < threshold)
                body.physics.velocity.x += impulse
            else if (isDirectionRotatedHorizontally() && abs(body.physics.velocity.y) < threshold)
                body.physics.velocity.y += impulse
            /*
            if (isDirectionRotatedVertically() && body.physics.velocity.x > -threshold)
                body.physics.velocity.x -= rawImpulse * delta * movementScalar * ConstVals.PPM
            else if (isDirectionRotatedHorizontally() && body.physics.velocity.y > -threshold)
                body.physics.velocity.y -= rawImpulse * delta * movementScalar * ConstVals.PPM
             */
        },
        onJustReleased = { poller ->
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "left actuator just released")
            if (!poller.isPressed(MegaControllerButtons.RIGHT)) running = false
        },
        onReleaseContinued = { poller, _ ->
            if (!poller.isPressed(MegaControllerButtons.RIGHT)) running = false
        })

    val right = ButtonActuator(
        onJustPressed = { _ ->
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "right actuator just pressed")
        },
        onPressContinued = { poller, delta ->
            if (!canMove || !ready || damaged || poller.isPressed(MegaControllerButtons.LEFT) || teleporting) {
                if (!poller.isPressed(MegaControllerButtons.LEFT)) running = false
                return@ButtonActuator
            }

            facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.LEFT else Facing.RIGHT
            if (directionRotation!!.equalsAny(Direction.DOWN, Direction.RIGHT)) swapFacing()

            if (isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.RIDING_CART)) return@ButtonActuator
            running = !isBehaviorActive(BehaviorType.WALL_SLIDING)

            val threshold = (if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_RUN_SPEED
            else MegamanValues.RUN_SPEED) * ConstVals.PPM

            val rawImpulse = if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
            else MegamanValues.RUN_IMPULSE

            val impulse = rawImpulse * delta * movementScalar * ConstVals.PPM * facing.value *
                    if (isBehaviorActive(BehaviorType.WALL_SLIDING)) -1f else 1f

            if (isDirectionRotatedVertically() && abs(body.physics.velocity.x) < threshold)
                body.physics.velocity.x += impulse
            else if (isDirectionRotatedHorizontally() && abs(body.physics.velocity.y) < threshold)
                body.physics.velocity.y += impulse
            /*
            if (isDirectionRotatedVertically() && body.physics.velocity.x < threshold)
                body.physics.velocity.x += impulse * delta * movementScalar * ConstVals.PPM
            else if (isDirectionRotatedHorizontally() && body.physics.velocity.y < threshold)
                body.physics.velocity.y += impulse * delta * movementScalar * ConstVals.PPM
             */
        },
        onJustReleased = { poller ->
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "right actuator just released")
            if (!poller.isPressed(MegaControllerButtons.LEFT)) running = false
        },
        onReleaseContinued = { poller, _ ->
            if (!poller.isPressed(MegaControllerButtons.LEFT)) running = false
        }
    )

    val attack = ButtonActuator(
        onPressContinued = { _, delta ->
            if (!ready || damaged || cameraRotating || teleporting || currentWeapon == MegamanWeapon.RUSH_JETPACK ||
                (!charging && !weaponHandler.canFireWeapon(currentWeapon, MegaChargeStatus.HALF_CHARGED)) ||
                (charging && !weaponHandler.canFireWeapon(currentWeapon, MegaChargeStatus.FULLY_CHARGED) ||
                        !has(MegaAbility.CHARGE_WEAPONS))
            ) {
                stopCharging()
                return@ButtonActuator
            }
            chargingTimer.update(delta)
        },
        onJustReleased = {
            if (damaged || cameraRotating || teleporting || !ready ||
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

    return ControllerComponent(MegaControllerButtons.LEFT pairTo { left },
        MegaControllerButtons.RIGHT pairTo { right },
        MegaControllerButtons.B pairTo { attack },
        MegaControllerButtons.SELECT pairTo { changeWeapon })
}
