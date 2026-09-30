package com.megaman.maverick.game.levels

import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon

/**
 * The items that Megaman has when a level starts. When applied, this replaces the player's current progress
 * (heart tanks, health tanks, enhancements, and weapons). [MegamanWeapon.MEGA_BUSTER] is always present.
 *
 * @param healthTanks each health tank mapped to its start fill value
 */
data class LoadoutOverride(
    val heartTanks: Set<MegaHeartTank> = emptySet(),
    val healthTanks: Map<MegaHealthTank, Int> = emptyMap(),
    val enhancements: Set<MegaEnhancement> = emptySet(),
    val weapons: Set<MegamanWeapon> = emptySet()
)
