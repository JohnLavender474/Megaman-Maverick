package com.megaman.maverick.game.entities.megaman.components

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonActuator
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.Megaman.Companion.TAG
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.megaman.input
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.teleporting
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs

const val MEGAMAN_CONTROLLER_COMPONENT_TAG = "MegamanControllerComponent"

internal fun Megaman.defineControllerComponent(): ControllerComponent {
    val left = ButtonActuator(
        onJustPressed = { GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "left actuator just pressed") },
        onPressContinued = { delta -> runLeft(delta) },
        onJustReleased = {
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "left actuator just released")
            if (!input.isPressed(MegaControllerButton.RIGHT)) {
                onJustStopRunning()
                onNotRunning()
            }
        },
        onReleaseContinued = { if (!input.isPressed(MegaControllerButton.RIGHT)) onNotRunning() }
    )

    val right = ButtonActuator(
        onJustPressed = { GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "right actuator just pressed") },
        onPressContinued = { delta -> runRight(delta) },
        onJustReleased = {
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "right actuator just released")
            if (!input.isPressed(MegaControllerButton.LEFT)) {
                onJustStopRunning()
                onNotRunning()
            }
        },
        onReleaseContinued = { if (!input.isPressed(MegaControllerButton.LEFT)) onNotRunning() }
    )

    val attack = ButtonActuator(
        onPressContinued = { delta ->
            if (!inPlay || stunned || damaged || teleporting || currentWeapon == MegamanWeapon.RUSH_JET ||
                (!charging && !weaponsHandler.canFireWeapon(currentWeapon, MegaChargeStatus.HALF_CHARGED)) ||
                (charging && !weaponsHandler.canFireWeapon(currentWeapon, MegaChargeStatus.FULLY_CHARGED))
            ) {
                stopCharging()
                return@ButtonActuator
            }

            val scalar = when {
                game.state.containsEnhancement(MegaEnhancement.FASTER_BUSTER_CHARGING) -> MegaEnhancement.FASTER_BUSTER_CHARGING_SCALAR
                else -> 1f
            }

            chargingTimer.update(delta * scalar)
        },
        onJustReleased = {
            if (stunned || damaged || game.camera.rotating || teleporting || !inPlay ||
                !weaponsHandler.canFireWeapon(currentWeapon, chargeStatus) ||
                game.levelState.roomTransition
            ) {
                GameLogger.debug(
                    MEGAMAN_CONTROLLER_COMPONENT_TAG,
                    "attack actuator just released, do not shoot: " +
                        "stunned=$stunned, damaged=$damaged, game.isCameraRotating=${game.camera.rotating}, " +
                        "teleporting=$teleporting, inPlay=$inPlay, " +
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

    val select = ButtonActuator(onJustReleased = {
        when (game.players.selectButtonAction) {
            SelectButtonAction.NEXT_WEAPON -> {
                if (!inPlay || damaged || teleporting) {
                    GameLogger.debug(
                        MEGAMAN_CONTROLLER_COMPONENT_TAG,
                        "select actuator just released, do nothing: inPlay=$inPlay, damaged=$damaged, teleporting=$teleporting"
                    )
                    return@ButtonActuator
                }

                GameLogger.debug(
                    MEGAMAN_CONTROLLER_COMPONENT_TAG,
                    "select actuator just released, attempt to set to next weapon"
                )

                setToNextWeapon()
            }

            else -> {}
        }
    })

    return ControllerComponent(
        MegaControllerButton.B pairTo attack,
        MegaControllerButton.LEFT pairTo left,
        MegaControllerButton.RIGHT pairTo right,
        MegaControllerButton.SELECT pairTo select
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

private fun Megaman.runLeft(delta: Float) {
    if (!inPlay || !canMove || damaged || teleporting ||
        input.isPressed(MegaControllerButton.RIGHT) ||
        (currentWeapon == MegamanWeapon.RODENT_CLAWS && shooting && bodyOverGround)
    ) {
        if (!input.isPressed(MegaControllerButton.RIGHT)) {
            running = false

            if (currentWeapon == MegamanWeapon.RODENT_CLAWS && shooting)
                facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.RIGHT else Facing.LEFT
        }
        return
    }

    facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.RIGHT else Facing.LEFT

    running = !isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.GROUND_SLIDING, BehaviorType.CLIMBING)
    if (!running) return

    runTime += delta

    val threshold = getMaxRunSpeed()

    val rawImpulse =
        if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
        else MegamanValues.RUN_IMPULSE

    var impulse = rawImpulse * delta * movementScalar * ConstVals.PPM *
        (if (isBehaviorActive(BehaviorType.WALL_SLIDING)) -1f else 1f)

    impulse *= if (body.isSensing(BodySense.FEET_ON_GROUND)) when {
        runTime < MegamanValues.RUN_1_TIME -> MegamanValues.RUN_1_SCALAR
        runTime < MegamanValues.RUN_2_TIME -> MegamanValues.RUN_2_SCALAR
        else -> MegamanValues.RUN_3_SCALAR
    } else 1f

    when {
        direction == Direction.UP && abs(body.physics.velocity.x) < threshold -> body.physics.velocity.x -= impulse
        direction == Direction.DOWN && abs(body.physics.velocity.x) < threshold -> body.physics.velocity.x += impulse
        direction == Direction.LEFT && abs(body.physics.velocity.y) < threshold -> body.physics.velocity.y -= impulse
        direction == Direction.RIGHT && abs(body.physics.velocity.y) < threshold -> body.physics.velocity.y += impulse
    }
}

private fun Megaman.runRight(delta: Float) {
    if (!inPlay || !canMove || damaged || teleporting ||
        input.isPressed(MegaControllerButton.LEFT) ||
        (currentWeapon == MegamanWeapon.RODENT_CLAWS && shooting && bodyOverGround)
    ) {
        if (!input.isPressed(MegaControllerButton.LEFT)) running = false

        if (currentWeapon == MegamanWeapon.RODENT_CLAWS && shooting)
            facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.LEFT else Facing.RIGHT

        return
    }

    facing = if (isBehaviorActive(BehaviorType.WALL_SLIDING)) Facing.LEFT else Facing.RIGHT

    running = !isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.GROUND_SLIDING, BehaviorType.CLIMBING)
    if (!running) return

    runTime += delta

    val threshold = getMaxRunSpeed()

    val rawImpulse =
        if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
        else MegamanValues.RUN_IMPULSE

    var impulse = rawImpulse * delta * movementScalar * ConstVals.PPM *
        (if (isBehaviorActive(BehaviorType.WALL_SLIDING)) -1f else 1f)
    impulse *= if (body.isSensing(BodySense.FEET_ON_GROUND)) when {
        runTime < MegamanValues.RUN_1_TIME -> MegamanValues.RUN_1_SCALAR
        runTime < MegamanValues.RUN_2_TIME -> MegamanValues.RUN_2_SCALAR
        else -> MegamanValues.RUN_3_SCALAR
    } else 1f

    when {
        direction == Direction.UP && abs(body.physics.velocity.x) < threshold -> body.physics.velocity.x += impulse
        direction == Direction.DOWN && abs(body.physics.velocity.x) < threshold -> body.physics.velocity.x -= impulse
        direction == Direction.LEFT && abs(body.physics.velocity.y) < threshold -> body.physics.velocity.y += impulse
        direction == Direction.RIGHT && abs(body.physics.velocity.y) < threshold -> body.physics.velocity.y -= impulse
    }
}

private fun Megaman.onJustStopRunning() {
    if (!shouldPreserveMomentum(true)) stopMomentum(true)
    runTime = 0f
}

private fun Megaman.onNotRunning() {
    running = false
}
