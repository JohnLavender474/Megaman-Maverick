package com.megaman.maverick.game.entities.megaman.behaviors

import com.megaman.maverick.game.behaviors.BehaviorType
import com.maverick.game.engine.behaviors.IBehavior
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.weapons.currentWeaponPrevents
import com.megaman.maverick.game.players.ClimbParams
import com.megaman.maverick.game.players.aButtonTask
import com.megaman.maverick.game.players.offGroundAButtonTask
import com.megaman.maverick.game.players.playerClimbBehavior

internal fun Megaman.climbBehavior(): IBehavior = playerClimbBehavior(
    this,
    ClimbParams(
        speed = { MegamanValues.CLIMB_VEL * movementScalar },
        blocked = { currentWeaponPrevents(BehaviorType.CLIMBING) },
        feetGrabAllowed = { true },
        holdStill = { shooting },
        leavesOnFloor = false,
        onStart = {
            aButtonTask = offGroundAButtonTask()
            canMakeLandSound = false
        },
        onEnd = { _ -> aButtonTask = offGroundAButtonTask() }
    )
)
