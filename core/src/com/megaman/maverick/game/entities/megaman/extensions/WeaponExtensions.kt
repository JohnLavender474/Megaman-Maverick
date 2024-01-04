package com.megaman.maverick.game.entities.megaman.extensions

import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon

fun Megaman.stopCharging() {
  stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
  chargingTimer.reset()
}

fun Megaman.shoot(): Boolean {
  val shot = weaponHandler.fireWeapon(currentWeapon, chargeStatus)
  if (shot) shootAnimTimer.reset()
  return shot
}

fun Megaman.canFireWeapon(weapon: MegamanWeapon) =
    weaponHandler.canFireWeapon(weapon, chargeStatus)

fun Megaman.canFireCurrentWeapon() = canFireWeapon(currentWeapon)
