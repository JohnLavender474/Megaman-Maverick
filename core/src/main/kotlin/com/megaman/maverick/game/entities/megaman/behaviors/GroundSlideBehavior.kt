package com.megaman.maverick.game.entities.megaman.behaviors

import com.maverick.game.engine.behaviors.IBehavior
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.players.GroundSlideParams
import com.megaman.maverick.game.players.playerGroundSlideBehavior
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

private fun Megaman.groundSlideSpeed(): Float {
    var speed = when {
        body.isSensing(BodySense.IN_WATER) -> MegamanValues.WATER_GROUND_SLIDE_VEL
        else -> MegamanValues.GROUND_SLIDE_VEL
    } * movementScalar
    // TODO: the boost is an enhancement read here until the loadout owns it (item 024)
    if (services.gameState.containsEnhancement(MegaEnhancement.GROUND_SLIDE_BOOST))
        speed *= MegaEnhancement.GROUND_SLIDE_BOOST_SCALAR
    return speed
}

internal fun Megaman.groundSlideBehavior(): IBehavior = playerGroundSlideBehavior(
    this,
    GroundSlideParams(
        minTime = MegamanValues.GROUND_SLIDE_MIN_TIME,
        maxTime = MegamanValues.GROUND_SLIDE_MAX_TIME,
        cooldown = MegamanValues.GROUND_SLIDE_COOLDOWN,
        speed = { groundSlideSpeed() },
        blocked = { currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting }
    )
)
