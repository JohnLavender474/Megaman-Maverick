package com.megaman.maverick.game.entities.megaman.components

import com.maverick.game.engine.common.GameLogger
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.Megaman.Companion.TAG
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.movement.RunParams

const val MEGAMAN_CONTROLLER_COMPONENT_TAG = "MegamanControllerComponent"

internal fun Megaman.setToNextWeapon() {
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
    movementScalar = { movementScalar }
)
