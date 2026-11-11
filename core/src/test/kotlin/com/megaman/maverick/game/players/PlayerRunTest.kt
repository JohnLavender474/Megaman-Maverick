package com.megaman.maverick.game.players

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.FreezeComponent
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.StunComponent
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.mockk

private const val DELTA = 0.1f

private class RunPlayer(services: MegaServices) : AbstractHealthEntity(services), IBodyEntity, IFaceable,
    IDirectional {

    override val damageNegotiator = PlayerDamageNegotiator(this)
    override var facing = Facing.RIGHT
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    var frame = InputFrame.RELEASED

    init {
        addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
        addComponent(LocksComponent(MegaLockGroup.entries.size))
        addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        addComponent(RunComponent())
        addComponent(StunComponent(1f, { true }, {}))
        addComponent(FreezeComponent(NeverThaw, {}))
        addComponent(BehaviorsComponent())
        addComponent(ControllerComponent().also { it.input = InputSource { frame } })
        damageTimer.setToEnd()
    }

    override fun getType() = EntityType.MEGAMAN
}

class PlayerRunTest : DescribeSpec({

    lateinit var player: RunPlayer
    var blocked = false
    var blockedFacing: Facing? = null

    fun params() = RunParams(
        impulse = 10f,
        iceImpulse = 2f,
        ramp1Time = 0.15f,
        ramp1Scalar = 0.5f,
        ramp2Time = 0.25f,
        ramp2Scalar = 0.8f,
        ramp3Scalar = 1f,
        maxSpeed = { 100f * ConstVals.PPM },
        movementScalar = { 1f },
        blocked = { blocked },
        onBlocked = { blockedFacing = it }
    )

    fun press(side: Facing) {
        player.frame = InputFrame.RELEASED.with(side.runButton(), ButtonStatus.PRESSED)
    }

    beforeEach {
        blocked = false
        blockedFacing = null
        player = RunPlayer(testServices(engine = mockk<GameEngine>(relaxed = true)))
    }

    describe("the shared run") {
        it("uses the impulse of the params, scaled by the ramp on the ground") {
            player.body.setBodySense(BodySense.FEET_ON_GROUND, true)
            press(Facing.LEFT)

            player.playerRun(DELTA, Facing.LEFT, params())

            player.body.physics.velocity.x shouldBe (-10f * DELTA * ConstVals.PPM * 0.5f plusOrMinus 0.001f)
            player.facing shouldBe Facing.LEFT
            player.running shouldBe true
        }

        it("uses the ice impulse of the params on ice") {
            player.body.setBodySense(BodySense.FEET_ON_ICE, true)
            press(Facing.RIGHT)

            player.playerRun(DELTA, Facing.RIGHT, params())

            player.body.physics.velocity.x shouldBe (2f * DELTA * ConstVals.PPM plusOrMinus 0.001f)
        }

        it("stops and calls the blocked hook with the run facing when the params block the run") {
            blocked = true
            press(Facing.LEFT)

            player.playerRun(DELTA, Facing.LEFT, params())

            player.running shouldBe false
            blockedFacing shouldBe Facing.LEFT
            player.facing shouldBe Facing.RIGHT
            player.body.physics.velocity.x shouldBe 0f
        }

        it("does not call the blocked hook when the run is free") {
            press(Facing.LEFT)

            player.playerRun(DELTA, Facing.LEFT, params())

            blockedFacing shouldBe null
        }
    }
})
