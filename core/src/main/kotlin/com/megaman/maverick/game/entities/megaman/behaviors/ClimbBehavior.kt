package com.megaman.maverick.game.entities.megaman.behaviors

import com.megaman.maverick.game.behaviors.BehaviorType
import com.maverick.game.engine.behaviors.IBehavior
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.weapons.currentWeaponPrevents
import com.megaman.maverick.game.players.ClimbParams
import com.megaman.maverick.game.players.playerClimbBehavior
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

private fun Megaman.waterTask() = when {
    body.isSensing(BodySense.IN_WATER) -> AButtonTask.SWIM
    else -> AButtonTask.AIR_DASH
}

internal fun Megaman.climbBehavior(): IBehavior = playerClimbBehavior(
    this,
    ClimbParams(
        speed = { MegamanValues.CLIMB_VEL * movementScalar },
        blocked = { currentWeaponPrevents(BehaviorType.CLIMBING) },
        feetGrabAllowed = { true },
        holdStill = { shooting },
        leavesOnFloor = false,
        onStart = {
            aButtonTask = waterTask()
            canMakeLandSound = false
        },
        onEnd = { _ -> aButtonTask = waterTask() }
    )
)
