package com.megaman.maverick.game.entities.megaman.components

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonActuator
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.players.playerSlot
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
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.WeaponChargeComponent
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.entities.state.shouldPreserveMomentum
import com.megaman.maverick.game.entities.state.stopMomentum
import com.megaman.maverick.game.entities.state.stunned
import com.megaman.maverick.game.entities.state.teleporting
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs
import com.megaman.maverick.game.players.bodyOverGround

const val MEGAMAN_CONTROLLER_COMPONENT_TAG = "MegamanControllerComponent"

internal fun Megaman.defineControllerComponent(): ControllerComponent {
    val left = ButtonActuator(
        onJustPressed = {
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "left actuator just pressed")
            require(RunComponent::class).lastPressedSide = Facing.LEFT
        },
        onPressContinued = { delta -> run(delta, Facing.LEFT) },
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
        onJustPressed = {
            GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "right actuator just pressed")
            require(RunComponent::class).lastPressedSide = Facing.RIGHT
        },
        onPressContinued = { delta -> run(delta, Facing.RIGHT) },
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
                services.gameState.containsEnhancement(MegaEnhancement.FASTER_BUSTER_CHARGING) -> MegaEnhancement.FASTER_BUSTER_CHARGING_SCALAR
                else -> 1f
            }

            require(WeaponChargeComponent::class).update(delta * scalar)
        },
        onJustReleased = {
            if (stunned || damaged || services.gameCamera.rotating || teleporting || !inPlay ||
                !weaponsHandler.canFireWeapon(currentWeapon, chargeStatus) ||
                services.levelState.roomTransition
            ) {
                GameLogger.debug(
                    MEGAMAN_CONTROLLER_COMPONENT_TAG,
                    "attack actuator just released, do not shoot: " +
                        "stunned=$stunned, damaged=$damaged, cameraRotating=${services.gameCamera.rotating}, " +
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
        when (services.players.settings(playerSlot!!).selectButtonAction) {
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

private fun Facing.button() = if (this == Facing.LEFT) MegaControllerButton.LEFT else MegaControllerButton.RIGHT

internal fun Megaman.isRunSidePressed(side: Facing) = input.isPressed(side.button()) &&
    (!input.isPressed(side.opposite().button()) || require(RunComponent::class).lastPressedSide == side)

internal fun Megaman.run(delta: Float, side: Facing) {
    if (!isRunSidePressed(side)) return

    val runState = require(RunComponent::class)

    val lastPressedJustReleased =
        runState.lastPressedSide != side && input.isJustReleased(side.opposite().button())
    val runFacing =
        if (isBehaviorActive(BehaviorType.WALL_SLIDING) && !lastPressedJustReleased) side.opposite() else side

    if (!inPlay || !canMove || damaged || teleporting ||
        (currentWeapon == MegamanWeapon.RODENT_CLAWS && shooting && bodyOverGround)
    ) {
        running = false

        if (currentWeapon == MegamanWeapon.RODENT_CLAWS && shooting) facing = runFacing

        return
    }

    facing = runFacing

    running = !isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.GROUND_SLIDING, BehaviorType.CLIMBING)
    if (!running) return

    runState.runTime += delta

    val threshold = getMaxRunSpeed()

    val rawImpulse =
        if (body.isSensing(BodySense.FEET_ON_ICE)) MegamanValues.ICE_RUN_IMPULSE
        else MegamanValues.RUN_IMPULSE

    var impulse = rawImpulse * delta * movementScalar * ConstVals.PPM * side.value

    impulse *= if (body.isSensing(BodySense.FEET_ON_GROUND)) when {
        runState.runTime < MegamanValues.RUN_1_TIME -> MegamanValues.RUN_1_SCALAR
        runState.runTime < MegamanValues.RUN_2_TIME -> MegamanValues.RUN_2_SCALAR
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
    require(RunComponent::class).runTime = 0f
}

private fun Megaman.onNotRunning() {
    running = false
}
