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

internal fun MegamanX.defineControllerComponent(): ControllerComponent {
    fun side(facing: Facing) = ButtonActuator(
        onJustPressed = { require(RunComponent::class).lastPressedSide = facing },
        onPressContinued = { delta -> run(delta, facing) },
        onJustReleased = { if (!input.isPressed(facing.opposite().button())) stopRunning() },
        onReleaseContinued = { if (!input.isPressed(facing.opposite().button())) running = false }
    )

    val attack = ButtonActuator(
        onJustPressed = { if (canAttack()) buster.shoot(MegaChargeStatus.NOT_CHARGED) },
        onPressContinued = { delta ->
            if (canAttack()) require(WeaponChargeComponent::class).update(delta) else stopCharging()
        },
        onJustReleased = {
            val status = chargeStatus
            if (canAttack() && status != MegaChargeStatus.NOT_CHARGED) buster.shoot(status)
            stopCharging()
        }
    )

    return ControllerComponent(
        MegaControllerButton.LEFT pairTo side(Facing.LEFT),
        MegaControllerButton.RIGHT pairTo side(Facing.RIGHT),
        MegaControllerButton.B pairTo attack
    )
}

private fun Facing.button() = if (this == Facing.LEFT) MegaControllerButton.LEFT else MegaControllerButton.RIGHT

private fun MegamanX.canAttack() = inPlay && !stunned && !damaged && !teleporting && !frozen &&
    !services.gameCamera.rotating && !services.levelState.roomTransition

internal fun MegamanX.run(delta: Float, side: Facing) {
    val runState = require(RunComponent::class)
    if (!input.isPressed(side.button()) ||
        (input.isPressed(side.opposite().button()) && runState.lastPressedSide != side)
    ) return

    if (!inPlay || !playerCanMove || damaged || teleporting) {
        running = false
        return
    }

    facing = side
    running = true

    val walkStart = bodyOverGround && runState.runTime < MegamanXValues.WALK_START_TIME
    runState.runTime += delta

    val speed = ConstVals.PPM * side.value * when {
        walkStart -> MegamanXValues.WALK_START_SPEED
        else -> MegamanXValues.WALK_SPEED
    }

    when (direction) {
        Direction.UP -> body.physics.velocity.x = speed
        Direction.DOWN -> body.physics.velocity.x = -speed
        Direction.LEFT -> body.physics.velocity.y = speed
        Direction.RIGHT -> body.physics.velocity.y = -speed
    }
}

private fun MegamanX.stopRunning() {
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

internal fun MegamanX.defineFreezeComponent() = FreezeComponent(
    MashThawRule(
        MegamanValues.HITS_TO_UNFREEZE,
        MegamanValues.FROZEN_PUSH_DUR,
        mashed = { MegamanValues.BUTTONS_TO_UNFREEZE.any { input.isJustReleased(it) } },
        onMash = { requestToPlaySound(SoundAsset.ICE_SHARD_1_SOUND, false) }
    ),
    onThaw = { IceShard.spawn5(body.getCenter()) }
)

internal fun MegamanX.defineStunComponent() = StunComponent(
    MegamanValues.STUN_DUR,
    onSurface = { body.isSensing(BodySense.FEET_ON_GROUND) },
    onStun = {
        // TODO: the stun bounce assumes that the body points up, as Megaman's does; the X6 levels have no gravity
        //  change (item 058)
        body.physics.velocity.let {
            it.x = MegamanValues.STUN_IMPULSE_X * ConstVals.PPM * -facing.value
            it.y = MegamanValues.STUN_IMPULSE_Y * ConstVals.PPM
        }
    }
)
