package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.exp

fun IBodyEntity.shouldPreserveMomentum(checkTimer: Boolean): Boolean {
    if (body.isSensing(BodySense.FEET_ON_SNOW)) return false
    if (body.isSensing(BodySense.FEET_ON_ICE)) return true
    return !checkTimer || !require(PostActionMomentumComponent::class).timer.isFinished()
}

fun IBodyEntity.preserveMomentum() {
    require(PostActionMomentumComponent::class).timer.reset()
}

fun IBodyEntity.stopMomentum(stopEntity: Boolean) {
    require(PostActionMomentumComponent::class).timer.setToEnd()
    if (stopEntity) when (body.direction) {
        Direction.UP, Direction.DOWN -> body.physics.velocity.x = 0f
        else -> body.physics.velocity.y = 0f
    }
}

fun IBodyEntity.killPostActionMomentum() {
    val momentum = require(PostActionMomentumComponent::class)
    val maxSpeed = momentum.maxSpeed()
    when (body.direction) {
        Direction.UP, Direction.DOWN ->
            body.physics.velocity.x = body.physics.velocity.x.coerceIn(-maxSpeed, maxSpeed)
        else ->
            body.physics.velocity.y = body.physics.velocity.y.coerceIn(-maxSpeed, maxSpeed)
    }
    momentum.pendingKill = false
}

fun IBodyEntity.requestMomentumKill() {
    require(PostActionMomentumComponent::class).pendingKill = true
}

fun IBodyEntity.updatePostActionMomentum(delta: Float, running: Boolean) {
    val momentum = require(PostActionMomentumComponent::class)

    if (momentum.overGround()) momentum.timer.update(delta)
    else if (momentum.facingBlock()) stopMomentum(!running)

    if (momentum.overGround() && momentum.pendingKill && !body.isSensing(BodySense.FEET_ON_ICE))
        killPostActionMomentum()

    if (!running && momentum.overGround() && !momentum.timer.isFinished() && !momentum.groundSliding() &&
        !body.isSensing(BodySense.FEET_ON_ICE)
    ) {
        val decay = exp(-momentum.resistance * delta)
        when (body.direction) {
            Direction.UP, Direction.DOWN -> body.physics.velocity.x *= decay
            else -> body.physics.velocity.y *= decay
        }
    }
}
