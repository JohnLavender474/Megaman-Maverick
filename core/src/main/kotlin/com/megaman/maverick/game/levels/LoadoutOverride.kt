package com.megaman.maverick.game.levels

import com.badlogic.gdx.utils.ObjectSet
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.weapons.WeaponDef

/**
 * The items that the player has when a level starts. When applied, this replaces the player's current progress
 * (heart tanks, health tanks, enhancements, and weapons). Each weapon goes to every character whose loadout has it;
 * a weapon with `alwaysOwned` is always present.
 *
 * @param healthTanks each health tank mapped to its start fill value
 */
data class LoadoutOverride(
    val heartTanks: Set<MegaHeartTank> = emptySet(),
    val healthTanks: Map<MegaHealthTank, Int> = emptyMap(),
    val enhancements: Set<MegaEnhancement> = emptySet(),
    val weapons: ObjectSet<WeaponDef<*>> = ObjectSet()
)
