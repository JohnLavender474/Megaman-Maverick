package com.megaman.maverick.game.entities.megaman.components

import com.maverick.game.engine.components.require
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.state.WeaponChargeComponent
import com.megaman.maverick.game.entities.state.stunned

internal fun Megaman.defineWeaponChargeComponent() = WeaponChargeComponent(
    MegamanValues.TIME_TO_HALFWAY_CHARGED,
    MegamanValues.TIME_TO_FULLY_CHARGED
) {
    requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND, false)
}

private val Megaman.canChargeCurrentWeapon: Boolean
    get() = weaponsHandler.isChargeable(currentWeapon)

internal val Megaman.chargeStatus: MegaChargeStatus
    get() = when {
        fullyCharged -> MegaChargeStatus.FULLY_CHARGED
        charging -> MegaChargeStatus.HALF_CHARGED
        else -> MegaChargeStatus.NOT_CHARGED
    }

internal val Megaman.charging: Boolean
    get() = canChargeCurrentWeapon && require(WeaponChargeComponent::class).half

internal val Megaman.halfCharged: Boolean
    get() = chargeStatus == MegaChargeStatus.HALF_CHARGED

internal val Megaman.fullyCharged: Boolean
    get() = canChargeCurrentWeapon && require(WeaponChargeComponent::class).full

internal fun Megaman.stopCharging() {
    stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
    require(WeaponChargeComponent::class).reset()
}

internal fun Megaman.clearChargeWhenHurt() {
    if (stunned || damaged) require(WeaponChargeComponent::class).reset()
}
