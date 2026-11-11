package com.megaman.maverick.game.entities.movement

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

private const val DELTA = 0.1f

class RunTest : DescribeSpec({

    lateinit var body: Body
    lateinit var run: RunComponent
    var maxSpeed = 100f * ConstVals.PPM

    fun params() = RunParams(
        impulse = 10f,
        iceImpulse = 2f,
        ramp1Time = 0.15f,
        ramp1Scalar = 0.5f,
        ramp2Time = 0.25f,
        ramp2Scalar = 0.8f,
        ramp3Scalar = 1f,
        maxSpeed = { maxSpeed },
        movementScalar = { 1f }
    )

    beforeEach {
        body = Body(BodyType.DYNAMIC)
        run = RunComponent()
        maxSpeed = 100f * ConstVals.PPM
    }

    describe("runStep") {
        it("uses the impulse of the params, scaled by the ramp on the ground") {
            body.setBodySense(BodySense.FEET_ON_GROUND, true)

            runStep(body, Direction.UP, run, Facing.LEFT, DELTA, params())

            body.physics.velocity.x shouldBe (-10f * DELTA * ConstVals.PPM * 0.5f plusOrMinus 0.001f)
            run.runTime shouldBe (DELTA plusOrMinus 0.0001f)
        }

        it("uses the ice impulse of the params on ice") {
            body.setBodySense(BodySense.FEET_ON_ICE, true)

            runStep(body, Direction.UP, run, Facing.RIGHT, DELTA, params())

            body.physics.velocity.x shouldBe (2f * DELTA * ConstVals.PPM plusOrMinus 0.001f)
        }

        it("pushes on the axis of the gravity direction, with the sign of that direction") {
            runStep(body, Direction.LEFT, run, Facing.RIGHT, DELTA, params())
            body.physics.velocity.y shouldBe (10f * DELTA * ConstVals.PPM plusOrMinus 0.001f)
            body.physics.velocity.x shouldBe 0f

            body.physics.velocity.setZero()
            runStep(body, Direction.DOWN, run, Facing.RIGHT, DELTA, params())
            body.physics.velocity.x shouldBe (-10f * DELTA * ConstVals.PPM plusOrMinus 0.001f)
        }

        it("does not push past the max speed") {
            maxSpeed = 1f
            body.physics.velocity.x = 2f

            runStep(body, Direction.UP, run, Facing.RIGHT, DELTA, params())

            body.physics.velocity.x shouldBe 2f
        }
    }

    describe("maxRunSpeed") {
        it("scales the max speed by PPM, and by the water scalar in water") {
            maxRunSpeed(body, 5f, 0.5f) shouldBe 5f * ConstVals.PPM

            body.setBodySense(BodySense.IN_WATER, true)
            maxRunSpeed(body, 5f, 0.5f) shouldBe 5f * ConstVals.PPM * 0.5f
        }
    }
})
