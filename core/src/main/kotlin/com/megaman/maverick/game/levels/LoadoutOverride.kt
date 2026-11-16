package com.megaman.maverick.game.levels

import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.weapons.WeaponDef

/**
 * The items that Megaman has when a level starts. When applied, this replaces the player's current progress
 * (heart tanks, health tanks, enhancements, and weapons). [MegamanWeapons.MEGA_BUSTER] is always present.
 *
 * @param healthTanks each health tank mapped to its start fill value
 */
data class LoadoutOverride(
    val heartTanks: Set<MegaHeartTank> = emptySet(),
    val healthTanks: Map<MegaHealthTank, Int> = emptyMap(),
    val enhancements: Set<MegaEnhancement> = emptySet(),
    val weapons: Set<WeaponDef> = emptySet()
)
