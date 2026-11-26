package com.megaman.maverick.game.entities.megamanx

import com.maverick.game.engine.common.enums.Direction
import com.megaman.maverick.game.players.SabreState
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonActuator
import com.maverick.game.engine.entities.input
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.controllers.MegaControllerButton
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

internal fun MegamanX.run(delta: Float, side: Facing) {
    val runState = require(RunComponent::class)
    if (!isRunSidePressed(side)) return

    if (!inPlay || !playerCanMove || damaged || teleporting || holdsSideSpeedForRun ||
        isBehaviorActive(BehaviorType.WALL_SLIDING) || sabre.state == SabreState.FLOOR
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
