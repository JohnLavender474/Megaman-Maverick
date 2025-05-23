package com.megaman.maverick.game.entities.megaman.extensions

import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.megaman.Megaman

fun Megaman.stopCharging() {
    stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
    chargingTimer.reset()
}

fun Megaman.shoot(): Boolean {
    val shot = weaponsHandler.fireWeapon(currentWeapon, chargeStatus)
    if (shot) shootAnimTimer.reset()
    return shot
}
