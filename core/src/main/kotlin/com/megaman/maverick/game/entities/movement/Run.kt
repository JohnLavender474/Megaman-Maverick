package com.megaman.maverick.game.entities.movement

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.world.body.Body
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs

class RunParams(
    val impulse: Float,
    val iceImpulse: Float,
    val ramp1Time: Float,
    val ramp1Scalar: Float,
    val ramp2Time: Float,
    val ramp2Scalar: Float,
    val ramp3Scalar: Float,
    val maxSpeed: () -> Float,
    val movementScalar: () -> Float
)

fun maxRunSpeed(body: Body, maxSpeed: Float, waterScalar: Float): Float {
    var threshold = maxSpeed * ConstVals.PPM
    if (body.isSensing(BodySense.IN_WATER)) threshold *= waterScalar
    return threshold
}

fun runStep(body: Body, direction: Direction, run: RunComponent, side: Facing, delta: Float, params: RunParams) {
    run.runTime += delta

    val threshold = params.maxSpeed()

    val rawImpulse = if (body.isSensing(BodySense.FEET_ON_ICE)) params.iceImpulse else params.impulse

    var impulse = rawImpulse * delta * params.movementScalar() * ConstVals.PPM * side.value

    impulse *= if (body.isSensing(BodySense.FEET_ON_GROUND)) when {
        run.runTime < params.ramp1Time -> params.ramp1Scalar
        run.runTime < params.ramp2Time -> params.ramp2Scalar
        else -> params.ramp3Scalar
    } else 1f

    when {
        direction == Direction.UP && abs(body.physics.velocity.x) < threshold -> body.physics.velocity.x += impulse
        direction == Direction.DOWN && abs(body.physics.velocity.x) < threshold -> body.physics.velocity.x -= impulse
        direction == Direction.LEFT && abs(body.physics.velocity.y) < threshold -> body.physics.velocity.y += impulse
        direction == Direction.RIGHT && abs(body.physics.velocity.y) < threshold -> body.physics.velocity.y -= impulse
    }
}
