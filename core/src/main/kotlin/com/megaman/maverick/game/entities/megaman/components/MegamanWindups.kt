package com.megaman.maverick.game.entities.megaman.components

import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.state.stunned
import com.megaman.maverick.game.players.stopWindup
import com.megaman.maverick.game.players.windups

internal val Megaman.chargeStatus: MegaChargeStatus
    get() = when {
        windupOf(currentWeapon) === busterCharge && windups.active === busterCharge -> busterCharge.status()
        else -> MegaChargeStatus.NOT_CHARGED
    }

internal val Megaman.holdingAxe: Boolean
    get() = windups.active === axeWindup

internal val Megaman.charging: Boolean
    get() = chargeStatus != MegaChargeStatus.NOT_CHARGED

internal val Megaman.halfCharged: Boolean
    get() = chargeStatus == MegaChargeStatus.HALF_CHARGED

internal val Megaman.fullyCharged: Boolean
    get() = chargeStatus == MegaChargeStatus.FULLY_CHARGED

internal fun Megaman.stopWindupNotOfCurrentWeapon() {
    if (windups.active != null && windups.active !== windupOf(currentWeapon)) stopWindup()
}

internal fun Megaman.clearChargeWhenHurt() {
    if (stunned || damaged) stopWindup()
}
