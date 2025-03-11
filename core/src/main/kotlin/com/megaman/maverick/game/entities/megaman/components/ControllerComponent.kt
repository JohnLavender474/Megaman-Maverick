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
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.Megaman.Companion.TAG
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.megaman.extensions.shoot
import com.megaman.maverick.game.entities.megaman.extensions.stopCharging
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

const val MEGAMAN_CONTROLLER_COMPONENT_TAG = "MegamanControllerComponent"

private fun Megaman.run() {
    var speed =
        if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_RUN_SPEED else MegamanValues.RUN_SPEED
    speed *= movementScalar * facing.value * ConstVals.PPM * if (isBehaviorActive(BehaviorType.WALL_SLIDING)) -1f else 1f

    GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "run(): speed=$speed")

    when {
        direction.isVertical() -> body.physics.velocity.x = speed
        else -> body.physics.velocity.y = speed
    }
}

internal fun Megaman.defineControllerComponent(): ControllerComponent {
    val left = ButtonActuator(
        onJustPressed = { _ -> GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "left actuator just pressed") },
        onPressContinued = { poller, delta ->
            if (!canMove || !ready || damaged || poller.isPressed(MegaControllerButton.RIGHT) || teleporting ||
                isBehaviorActive(BehaviorType.GROUND_SLIDING)
            ) {
                if (!poller.isPressed(MegaControllerButton.RIGHT)) running = false
                return@ButtonActuator
            }

            facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.RIGHT else Facing.LEFT
            if (direction.equalsAny(Direction.DOWN, Direction.RIGHT)) swapFacing()

            if (isBehaviorActive(BehaviorType.CLIMBING)) return@ButtonActuator

            running = !isBehaviorActive(BehaviorType.WALL_SLIDING)
            if (running) run()
        },
        onJustReleased = { poller ->
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "left actuator just released")
            if (!poller.isPressed(MegaControllerButton.RIGHT)) running = false
        },
        onReleaseContinued = { poller, _ -> if (!poller.isPressed(MegaControllerButton.RIGHT)) running = false }
    )

    val right = ButtonActuator(
        onJustPressed = { _ -> GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "right actuator just pressed") },
        onPressContinued = { poller, delta ->
            if (!canMove || !ready || damaged || poller.isPressed(MegaControllerButton.LEFT) || teleporting ||
                isBehaviorActive(BehaviorType.GROUND_SLIDING)
            ) {
                if (!poller.isPressed(MegaControllerButton.LEFT)) running = false
                return@ButtonActuator
            }

            facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.LEFT else Facing.RIGHT
            if (direction.equalsAny(Direction.DOWN, Direction.RIGHT)) swapFacing()

            if (isBehaviorActive(BehaviorType.CLIMBING)) return@ButtonActuator

            running = !isBehaviorActive(BehaviorType.WALL_SLIDING)
            if (running) run()
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
            if (!ready || stunned || damaged || teleporting || currentWeapon == MegamanWeapon.RUSH_JETPACK ||
                (!charging && !weaponsHandler.canFireWeapon(currentWeapon, MegaChargeStatus.HALF_CHARGED)) ||
                (charging && !weaponsHandler.canFireWeapon(currentWeapon, MegaChargeStatus.FULLY_CHARGED))
            ) {
                stopCharging()
                return@ButtonActuator
            }

            val scalar = when {
                hasEnhancement(MegaEnhancement.FASTER_BUSTER_CHARGING) -> MegaEnhancement.FASTER_BUSTER_CHARGING_SCALAR
                else -> 1f
            }

            chargingTimer.update(delta * scalar)
        },
        onJustReleased = {
            if (stunned || damaged || game.isCameraRotating() || teleporting || !ready ||
                !weaponsHandler.canFireWeapon(currentWeapon, chargeStatus) ||
                game.isProperty(ConstKeys.ROOM_TRANSITION, true)
            ) {
                GameLogger.debug(
                    MEGAMAN_CONTROLLER_COMPONENT_TAG,
                    "attack actuator just released, do not shoot: " +
                        "stunned=$stunned, damaged=$damaged, game.isCameraRotating=${game.isCameraRotating()}, " +
                        "teleporting=$teleporting, ready=$ready, " +
                        "canFireWeapon=${weaponsHandler.canFireWeapon(currentWeapon, chargeStatus)}"
                )

                stopCharging()

                return@ButtonActuator
            }

            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "attack actuator just released, shoot")

            shoot()

            stopCharging()
        }
    )

    val select = ButtonActuator(onJustReleased = { _ ->
        if (!ready || damaged || teleporting) {
            GameLogger.debug(
                MEGAMAN_CONTROLLER_COMPONENT_TAG,
                "select actuator just released, do nothing: ready=$ready, damaged=$damaged, teleporting=$teleporting"
            )
            return@ButtonActuator
        }

        GameLogger.debug(
            MEGAMAN_CONTROLLER_COMPONENT_TAG,
            "select actuator just released, attempt to set to next weapon"
        )

        setToNextWeapon()
    })

    return ControllerComponent(
        MegaControllerButton.LEFT pairTo { left },
        MegaControllerButton.RIGHT pairTo { right },
        MegaControllerButton.B pairTo { attack },
        MegaControllerButton.SELECT pairTo { select }
    )
}

private fun Megaman.setToNextWeapon() {
    val start = currentWeapon.ordinal
    var temp = currentWeapon.ordinal + 1

    var nextWeapon = currentWeapon

    while (temp != start) {
        if (temp >= MegamanWeapon.entries.size) temp = 0

        val weapon = MegamanWeapon.entries[temp]
        if (weaponsHandler.hasWeapon(weapon)) {
            nextWeapon = weapon
            break
        }

        temp++
    }

    if (nextWeapon == currentWeapon)
        GameLogger.debug(TAG, "setToNextWeapon(): no next weapon, stay on current weapon: $currentWeapon")
    else {
        GameLogger.debug(TAG, "setToNextWeapon(): set to next weapon: next=$nextWeapon, current=$currentWeapon")
        currentWeapon = nextWeapon
    }
}
