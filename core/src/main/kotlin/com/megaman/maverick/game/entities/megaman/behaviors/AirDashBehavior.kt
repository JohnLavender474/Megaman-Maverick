package com.megaman.maverick.game.entities.megaman.behaviors

import com.megaman.maverick.game.behaviors.BehaviorType
import com.maverick.game.engine.behaviors.IBehavior
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.weapons.currentWeaponPrevents
import com.megaman.maverick.game.players.AButtonTask
import com.megaman.maverick.game.players.AirDashParams
import com.megaman.maverick.game.players.aButtonTask
import com.megaman.maverick.game.players.aButtonTaskOnAirDashStart
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
            currentWeaponPrevents(BehaviorType.AIR_DASHING) ||
                (currentWeapon.airDash == null && bodyOverGround) ||
                currentWeapon.airDash?.endsWhen?.invoke(this) == true
        },
        canStart = { aButtonTask == AButtonTask.AIR_DASH && airDashInputJustPressed(this) },
        held = { airDashInputPressed(this) },
        startDelay = 0f,
        minTime = MegamanValues.AIR_DASH_MIN_TIME,
        maxTime = {
            currentWeapon.airDash?.maxTime ?: MegamanValues.AIR_DASH_MAX_TIME
        },
        speed = {
            var speed = movementScalar *
                if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_VEL else MegamanValues.AIR_DASH_VEL
            currentWeapon.airDash?.let { speed *= it.speedScalar }
            speed
        },
        decel = { 0f },
        minSpeed = 0f,
        endBump = {
            if (body.isSensing(BodySense.IN_WATER)) MegamanValues.WATER_AIR_DASH_END_BUMP
            else MegamanValues.AIR_DASH_END_BUMP
        },
        onStart = {
            aButtonTaskOnAirDashStart()
            airDashDirection = direction
        },
        onEnd = { currentWeapon.airDash?.onEnd?.invoke(this) }
    )
)
