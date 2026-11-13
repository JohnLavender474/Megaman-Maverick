package com.megaman.maverick.game.entities.megamanx

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonActuator
import com.maverick.game.engine.entities.input
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.players.bodyOverGround
import com.megaman.maverick.game.players.playerCanMove
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.players.isRunSidePressed
import com.megaman.maverick.game.players.runButton
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.behaviors.BehaviorType

internal fun MegamanX.canAttack() = inPlay && !stunned && !damaged && !teleporting && !frozen &&
    !services.gameCamera.rotating && !services.levelState.roomTransition

internal fun MegamanX.run(delta: Float, side: Facing) {
    val runState = require(RunComponent::class)
    if (!isRunSidePressed(side)) return

    if (!inPlay || !playerCanMove || damaged || teleporting || holdsSideSpeedForRun ||
        isBehaviorActive(BehaviorType.WALL_SLIDING) || sabre.kind == SabreKind.FLOOR
    ) {
        running = false
        return
    }

    facing = side
    if (isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.CROUCHING)) {
        running = false
        return
    }
    running = true

    val walkStart = bodyOverGround && runState.runTime < MegamanXValues.WALK_START_TIME
    runState.runTime += delta

    val speed = ConstVals.PPM * side.value * when {
        walkStart -> MegamanXValues.WALK_START_SPEED
        !bodyOverGround && dashSpeedInAir -> MegamanXValues.DASH_AIR_SPEED
        else -> MegamanXValues.WALK_SPEED
    }

    when (direction) {
        Direction.UP -> body.physics.velocity.x = speed
        Direction.DOWN -> body.physics.velocity.x = -speed
        Direction.LEFT -> body.physics.velocity.y = speed
        Direction.RIGHT -> body.physics.velocity.y = -speed
    }
}

internal fun MegamanX.stopRunning() {
    running = false
    require(RunComponent::class).runTime = 0f
}

internal val MegamanX.walkStarting: Boolean
    get() = running && require(RunComponent::class).runTime < MegamanXValues.WALK_START_TIME

internal fun MegamanX.defineWeaponChargeComponent() = WeaponChargeComponent(
    MegamanXValues.TIME_TO_HALF_CHARGE,
    MegamanXValues.TIME_TO_FULL_CHARGE
) {
    requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND, false)
}

internal val MegamanX.chargeStatus: MegaChargeStatus
    get() {
        val charge = require(WeaponChargeComponent::class)
        return when {
            charge.full -> MegaChargeStatus.FULLY_CHARGED
            charge.half -> MegaChargeStatus.HALF_CHARGED
            else -> MegaChargeStatus.NOT_CHARGED
        }
    }

internal fun MegamanX.stopCharging() {
    stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
    require(WeaponChargeComponent::class).reset()
}

