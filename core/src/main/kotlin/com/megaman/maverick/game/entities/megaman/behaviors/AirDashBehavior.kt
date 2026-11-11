package com.megaman.maverick.game.entities.megaman.behaviors

import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.entities.input
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.players.AirDashParams
import com.megaman.maverick.game.players.airDashInputJustPressed
import com.megaman.maverick.game.players.airDashInputPressed
import com.megaman.maverick.game.players.bodyOverGround
import com.megaman.maverick.game.players.playerAirDashBehavior
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

internal fun Megaman.airDashBehavior(): IBehavior = playerAirDashBehavior(
    this,
    AirDashParams(
        blocked = {
            (currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting) ||
                (currentWeapon != MegamanWeapon.RUSH_JET && bodyOverGround) ||
                (currentWeapon == MegamanWeapon.RUSH_JET &&
                    (weaponsHandler.isDepleted(MegamanWeapon.RUSH_JET) || input.isPressed(MegaControllerButton.UP)))
        },
        canStart = { aButtonTask == AButtonTask.AIR_DASH && airDashInputJustPressed(this) },
        held = { airDashInputPressed(this) },
        startDelay = 0f,
        minTime = MegamanValues.AIR_DASH_MIN_TIME,
        maxTime = {
            if (currentWeapon == MegamanWeapon.RUSH_JET) MegamanValues.RUSH_JET_AIR_DASH_MAX_TIME
            else MegamanValues.AIR_DASH_MAX_TIME
        },
        speed = {
            var speed = movementScalar *
                if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_VEL else MegamanValues.AIR_DASH_VEL
            if (currentWeapon == MegamanWeapon.RUSH_JET) speed *= MegamanValues.RUSH_JET_AIR_DASH_SCALAR
            speed
        },
        decel = { 0f },
        minSpeed = 0f,
        endBump = {
            if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_END_BUMP
            else MegamanValues.AIR_DASH_END_BUMP
        },
        onStart = {
            aButtonTask = AButtonTask.JUMP
            airDashConsumed = true
            airDashDirection = direction
        },
        onEnd = { if (currentWeapon == MegamanWeapon.RUSH_JET) jetpackHandler.reset() }
    )
)
