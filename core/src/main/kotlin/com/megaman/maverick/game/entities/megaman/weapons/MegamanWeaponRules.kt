package com.megaman.maverick.game.entities.megaman.weapons

import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman

internal fun Megaman.currentWeaponPrevents(move: BehaviorType) =
    currentWeapon.preventsWhileFiring(move) && shooting
