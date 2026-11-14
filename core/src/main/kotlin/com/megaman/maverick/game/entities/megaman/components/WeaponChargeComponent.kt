package com.megaman.maverick.game.entities.megaman.components

import com.maverick.game.engine.components.require
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.state.WeaponChargeComponent
import com.megaman.maverick.game.entities.state.stunned
import com.megaman.maverick.game.players.playerChargeComponent
import com.megaman.maverick.game.players.playerChargeStatus

internal fun Megaman.defineWeaponChargeComponent() =
    playerChargeComponent(this, MegamanValues.TIME_TO_HALFWAY_CHARGED, MegamanValues.TIME_TO_FULLY_CHARGED)

private val Megaman.canChargeCurrentWeapon: Boolean
    get() = weaponsHandler.isChargeable(currentWeapon)

internal val Megaman.chargeStatus: MegaChargeStatus
    get() = if (canChargeCurrentWeapon) playerChargeStatus else MegaChargeStatus.NOT_CHARGED

internal val Megaman.charging: Boolean
    get() = canChargeCurrentWeapon && require(WeaponChargeComponent::class).half

internal val Megaman.halfCharged: Boolean
    get() = chargeStatus == MegaChargeStatus.HALF_CHARGED

internal val Megaman.fullyCharged: Boolean
    get() = canChargeCurrentWeapon && require(WeaponChargeComponent::class).full

internal fun Megaman.clearChargeWhenHurt() {
    if (stunned || damaged) require(WeaponChargeComponent::class).reset()
}
