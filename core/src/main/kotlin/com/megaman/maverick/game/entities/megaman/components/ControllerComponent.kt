package com.megaman.maverick.game.entities.megaman.components

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonActuator
import com.maverick.game.engine.players.playerSlot
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.Megaman.Companion.TAG
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.state.WeaponChargeComponent
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.stunned
import com.megaman.maverick.game.entities.state.teleporting
import com.megaman.maverick.game.players.bodyOverGround
import com.megaman.maverick.game.players.RunParams
import com.megaman.maverick.game.players.playerRunButton

const val MEGAMAN_CONTROLLER_COMPONENT_TAG = "MegamanControllerComponent"

internal fun Megaman.defineControllerComponent(): ControllerComponent {
    val left = playerRunButton(Facing.LEFT, runParams)
    val right = playerRunButton(Facing.RIGHT, runParams)

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

internal fun Megaman.defineRunParams() = RunParams(
    impulse = MegamanValues.RUN_IMPULSE,
    iceImpulse = MegamanValues.ICE_RUN_IMPULSE,
    ramp1Time = MegamanValues.RUN_1_TIME,
    ramp1Scalar = MegamanValues.RUN_1_SCALAR,
    ramp2Time = MegamanValues.RUN_2_TIME,
    ramp2Scalar = MegamanValues.RUN_2_SCALAR,
    ramp3Scalar = MegamanValues.RUN_3_SCALAR,
    maxSpeed = { getMaxRunSpeed() },
    movementScalar = { movementScalar },
    blocked = { currentWeapon == MegamanWeapon.RODENT_CLAWS && shooting && bodyOverGround },
    onBlocked = { runFacing -> if (currentWeapon == MegamanWeapon.RODENT_CLAWS && shooting) facing = runFacing }
)
