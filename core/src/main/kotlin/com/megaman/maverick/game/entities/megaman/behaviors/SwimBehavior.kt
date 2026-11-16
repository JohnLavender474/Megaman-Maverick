package com.megaman.maverick.game.entities.megaman.behaviors

import com.maverick.game.engine.behaviors.IBehavior
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.weapons.currentWeaponPrevents
import com.megaman.maverick.game.players.AButtonTask
import com.megaman.maverick.game.players.SwimParams
import com.megaman.maverick.game.players.aButtonTask
import com.megaman.maverick.game.players.playerSwimBehavior

internal fun Megaman.swimBehavior(): IBehavior = playerSwimBehavior(
    this,
    SwimParams(
        time = MegamanValues.SWIM_TIMER,
        speed = {
            when {
                services.gameState.containsEnhancement(MegaEnhancement.JUMP_BOOST) ->
                    swimVel * MegaEnhancement.JUMP_BOOST_SCALAR

                else -> swimVel
            }
        },
        blocked = { currentWeaponPrevents(BehaviorType.SWIMMING) },
        canStart = { aButtonTask == AButtonTask.SWIM }
    )
)
