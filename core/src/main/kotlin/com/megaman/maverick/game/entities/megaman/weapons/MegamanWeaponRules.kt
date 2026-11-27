package com.megaman.maverick.game.entities.megaman.weapons

import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.weapons.WeaponAirDash
import com.megaman.maverick.game.weapons.WeaponDef

internal fun Megaman.currentWeaponPrevents(move: BehaviorType) =
    currentWeapon.preventsWhileFiring(move) && shooting

internal val Megaman.weaponMovesReady: Boolean
    get() = true

internal val Megaman.drawnWeapon: WeaponDef<Megaman>
    get() = if (weaponMovesReady) currentWeapon else MegamanWeapons.MEGA_BUSTER

internal val Megaman.jetpackEnabled: Boolean
    get() = currentWeapon.enablesJetpack && weaponMovesReady

internal val Megaman.weaponAirDash: WeaponAirDash<Megaman>?
    get() = if (weaponMovesReady) currentWeapon.airDash else null
