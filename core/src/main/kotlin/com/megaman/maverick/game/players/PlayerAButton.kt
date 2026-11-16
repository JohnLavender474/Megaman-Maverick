package com.megaman.maverick.game.players

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

enum class AButtonTask {
    JUMP,
    SWIM,
    AIR_DASH
}

class PlayerAButtonComponent : IGameComponent {
    var task = AButtonTask.JUMP
    var airDashConsumed = false
}

var MegaPlayer.aButtonTask: AButtonTask
    get() = require(PlayerAButtonComponent::class).task
    set(value) {
        require(PlayerAButtonComponent::class).task = value
    }

fun MegaPlayer.offGroundAButtonTask() = when {
    body.isSensing(BodySense.IN_WATER) -> AButtonTask.SWIM
    else -> AButtonTask.AIR_DASH
}

fun MegaPlayer.setAirDashTaskOutOfWater() {
    if (!body.isSensing(BodySense.IN_WATER)) aButtonTask = AButtonTask.AIR_DASH
}

fun MegaPlayer.resetAButtonTask() {
    val component = require(PlayerAButtonComponent::class)
    component.task = AButtonTask.JUMP
    component.airDashConsumed = false
}

fun MegaPlayer.aButtonTaskOnAirDashStart() {
    val component = require(PlayerAButtonComponent::class)
    component.task = AButtonTask.JUMP
    component.airDashConsumed = true
}

fun MegaPlayer.aButtonTaskOnWater(state: ProcessState) {
    when (state) {
        ProcessState.BEGIN, ProcessState.CONTINUE -> if (
            !body.isSensing(BodySense.FEET_ON_GROUND) && !isBehaviorActive(BehaviorType.WALL_SLIDING)
        ) aButtonTask = AButtonTask.SWIM

        ProcessState.END -> aButtonTask = AButtonTask.AIR_DASH
    }
}

fun MegaPlayer.aButtonTaskOnGravitySwitch() {
    aButtonTask = when {
        body.isSensing(BodySense.FEET_ON_GROUND) -> AButtonTask.JUMP
        else -> AButtonTask.AIR_DASH
    }
}

fun MegaPlayer.updateAButtonTask() {
    val component = require(PlayerAButtonComponent::class)
    if (bodyOverGround) component.airDashConsumed = false

    if (!bodyOverGround && component.task == AButtonTask.JUMP &&
        !isAnyBehaviorActive(
            BehaviorType.WALL_SLIDING,
            BehaviorType.CLIMBING,
            BehaviorType.JUMPING
        )
    ) component.task = when {
        body.isSensing(BodySense.IN_WATER) -> AButtonTask.SWIM
        !component.airDashConsumed -> AButtonTask.AIR_DASH
        else -> AButtonTask.JUMP
    }

    if (bodyOverGround && component.task == AButtonTask.AIR_DASH &&
        !body.isSensing(BodySense.IN_WATER) &&
        !isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.AIR_DASHING)
    ) component.task = AButtonTask.JUMP
}
