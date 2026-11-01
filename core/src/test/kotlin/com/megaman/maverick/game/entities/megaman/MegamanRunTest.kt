package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity
import com.megaman.maverick.game.entities.megaman.components.run
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.TeleportableComponent
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private const val DELTA = 0.1f
private const val TOLERANCE = 0.001f

class MegamanRunTest : DescribeSpec({

    lateinit var megaman: Megaman
    lateinit var body: Body
    lateinit var behaviors: BehaviorsComponent
    var frame = InputFrame.RELEASED

    val airImpulse = MegamanValues.RUN_IMPULSE * DELTA * ConstVals.PPM

    fun press(vararg buttons: MegaControllerButton) {
        frame = InputFrame.RELEASED
        buttons.forEach { frame = frame.with(it, ButtonStatus.PRESSED) }
    }

    fun run(side: Facing, delta: Float = DELTA) {
        press(if (side == Facing.LEFT) MegaControllerButton.LEFT else MegaControllerButton.RIGHT)
        megaman.run(delta, side)
    }

    fun runWithBothPressed(side: Facing) {
        press(MegaControllerButton.LEFT, MegaControllerButton.RIGHT)
        megaman.run(DELTA, side)
    }

    fun setBehaviorActive(type: BehaviorType) {
        val behavior = mockk<IBehavior>(relaxed = true)
        every { behavior.isActive() } returns true
        behaviors.addBehavior(type, behavior)
    }

    fun stopped() = body.physics.velocity.x == 0f && body.physics.velocity.y == 0f

    fun slashOnGround() {
        megaman.currentWeapon = MegamanWeapon.RODENT_CLAWS
        megaman.shooting = true
        body.putProperty(ConstKeys.BODY_OVER_GROUND, { true })
    }

    beforeEach {
        frame = InputFrame.RELEASED

        val services = testServices(engine = mockk<GameEngine>(relaxed = true))
        services.levelState.currentLevel = LevelDefinition.DESERT_MAN

        megaman = Megaman(services)
        megaman.weaponsHandler = mockk(relaxed = true)
        megaman.currentWeapon = MegamanWeapon.MEGA_BUSTER
        megaman.facing = Facing.RIGHT

        body = Body(BodyType.DYNAMIC)
        megaman.addComponent(BodyComponent(body))
        megaman.addComponent(ControllerComponent().also { it.input = com.maverick.game.engine.input.InputSource { frame } })
        megaman.addComponent(LocksComponent(MegaLockGroup.entries.size))
        megaman.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        megaman.addComponent(TeleportableComponent())
        megaman.addComponent(RunComponent())
        behaviors = BehaviorsComponent()
        megaman.addComponent(behaviors)
    }

    afterEach { unmockkAll() }

    describe("the impulse") {
        data class Case(val direction: Direction, val side: Facing, val x: Float, val y: Float)

        listOf(
            Case(Direction.UP, Facing.LEFT, -1f, 0f),
            Case(Direction.UP, Facing.RIGHT, 1f, 0f),
            Case(Direction.DOWN, Facing.LEFT, 1f, 0f),
            Case(Direction.DOWN, Facing.RIGHT, -1f, 0f),
            Case(Direction.LEFT, Facing.LEFT, 0f, -1f),
            Case(Direction.LEFT, Facing.RIGHT, 0f, 1f),
            Case(Direction.RIGHT, Facing.LEFT, 0f, 1f),
            Case(Direction.RIGHT, Facing.RIGHT, 0f, -1f)
        ).forEach { case ->
            it("has the right sign for ${case.side} with the body ${case.direction}") {
                megaman.facing = if (case.side == Facing.LEFT) Facing.RIGHT else Facing.LEFT
                body.direction = case.direction

                run(case.side)

                body.physics.velocity.x shouldBe (case.x * airImpulse plusOrMinus TOLERANCE)
                body.physics.velocity.y shouldBe (case.y * airImpulse plusOrMinus TOLERANCE)
                megaman.facing shouldBe case.side
                megaman.running shouldBe true
            }
        }

        it("uses the ice impulse on ice") {
            body.setBodySense(BodySense.FEET_ON_ICE, true)

            run(Facing.RIGHT)

            body.physics.velocity.x shouldBe
                (MegamanValues.ICE_RUN_IMPULSE * DELTA * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("is scaled by the movement scalar") {
            megaman.addComponent(com.maverick.game.engine.animations.AnimationsComponent())
            megaman.movementScalar = 0.5f

            run(Facing.RIGHT)

            body.physics.velocity.x shouldBe (airImpulse * 0.5f plusOrMinus TOLERANCE)
        }

        it("stops at the max run speed") {
            val maxSpeed = MegamanValues.RUN_MAX_SPEED * ConstVals.PPM
            body.physics.velocity.x = maxSpeed

            run(Facing.RIGHT)

            body.physics.velocity.x shouldBe maxSpeed
        }

        it("still applies just under the max run speed") {
            val nearMax = MegamanValues.RUN_MAX_SPEED * ConstVals.PPM - 1f
            body.physics.velocity.x = nearMax

            run(Facing.RIGHT)

            body.physics.velocity.x shouldBe (nearMax + airImpulse plusOrMinus TOLERANCE)
        }
    }

    describe("the run ramp") {
        fun impulseAt(delta: Float, scalar: Float) = MegamanValues.RUN_IMPULSE * delta * ConstVals.PPM * scalar

        it("goes through the 3 scalars on the ground") {
            body.setBodySense(BodySense.FEET_ON_GROUND, true)

            run(Facing.RIGHT, 0.05f)
            body.physics.velocity.x shouldBe (impulseAt(0.05f, MegamanValues.RUN_1_SCALAR) plusOrMinus TOLERANCE)

            body.physics.velocity.x = 0f
            run(Facing.RIGHT, 0.1f)
            body.physics.velocity.x shouldBe (impulseAt(0.1f, MegamanValues.RUN_2_SCALAR) plusOrMinus TOLERANCE)

            body.physics.velocity.x = 0f
            run(Facing.RIGHT, 0.1f)
            body.physics.velocity.x shouldBe (impulseAt(0.1f, MegamanValues.RUN_3_SCALAR) plusOrMinus TOLERANCE)
        }

        it("has no scalar in the air") {
            run(Facing.RIGHT, 0.05f)

            body.physics.velocity.x shouldBe (impulseAt(0.05f, 1f) plusOrMinus TOLERANCE)
        }
    }

    describe("the guard") {
        listOf(Facing.LEFT, Facing.RIGHT).forEach { side ->
            describe("for $side") {
                fun assertStops(setup: () -> Unit) {
                    megaman.running = true
                    setup()

                    run(side)

                    stopped() shouldBe true
                    megaman.running shouldBe false
                }

                it("stops with a movement lock") {
                    assertStops { megaman.lock(MegaLockGroup.MOVEMENT, MegaLockReason.TELEPORT) }
                }

                it("stops during a teleport") {
                    assertStops {
                        megaman.getComponent(TeleportableComponent::class)!!.teleporter = mockk<ITeleporterEntity>()
                    }
                }

                it("reads no impulse out of play, and running is false") {
                    megaman.getComponent(PlayPhaseComponent::class)!!.phase = PlayPhase.PRE_PLAY

                    run(side)

                    stopped() shouldBe true
                    megaman.running shouldBe false
                }

                it("leaves the facing with no shot") {
                    val other = if (side == Facing.LEFT) Facing.RIGHT else Facing.LEFT
                    megaman.facing = other
                    megaman.lock(MegaLockGroup.MOVEMENT, MegaLockReason.TELEPORT)

                    run(side)

                    megaman.facing shouldBe other
                }

                it("keeps running as it is when the opposite button is pressed") {
                    megaman.running = true

                    runWithBothPressed(side)

                    stopped() shouldBe true
                    megaman.running shouldBe true
                }
            }
        }
    }

    describe("behaviors that stop the run") {
        listOf(BehaviorType.WALL_SLIDING, BehaviorType.GROUND_SLIDING, BehaviorType.CLIMBING).forEach { type ->
            it("$type gives no impulse and no run") {
                setBehaviorActive(type)

                run(Facing.RIGHT)

                stopped() shouldBe true
                megaman.running shouldBe false
            }
        }

        it("a wall slide turns the facing away from the button") {
            setBehaviorActive(BehaviorType.WALL_SLIDING)

            run(Facing.RIGHT)
            megaman.facing shouldBe Facing.LEFT

            run(Facing.LEFT)
            megaman.facing shouldBe Facing.RIGHT
        }

        it("a ground slide keeps the facing of the button") {
            setBehaviorActive(BehaviorType.GROUND_SLIDING)

            run(Facing.LEFT)

            megaman.facing shouldBe Facing.LEFT
        }
    }

    describe("RODENT_CLAWS") {
        it("stops the run on the ground during a shot, and sets the facing") {
            slashOnGround()

            run(Facing.LEFT)
            stopped() shouldBe true
            megaman.facing shouldBe Facing.LEFT

            run(Facing.RIGHT)
            stopped() shouldBe true
            megaman.facing shouldBe Facing.RIGHT
        }

        it("turns the facing during a wall slide") {
            slashOnGround()
            setBehaviorActive(BehaviorType.WALL_SLIDING)

            run(Facing.LEFT)
            megaman.facing shouldBe Facing.RIGHT

            run(Facing.RIGHT)
            megaman.facing shouldBe Facing.LEFT
        }

        it("does not stop the run in the air") {
            megaman.currentWeapon = MegamanWeapon.RODENT_CLAWS
            megaman.shooting = true
            body.putProperty(ConstKeys.BODY_OVER_GROUND, { false })

            run(Facing.RIGHT)

            body.physics.velocity.x shouldBe (airImpulse plusOrMinus TOLERANCE)
        }

        it("with both buttons pressed during a shot, the right side sets the facing") {
            slashOnGround()
            megaman.facing = Facing.LEFT

            runWithBothPressed(Facing.RIGHT)

            megaman.facing shouldBe Facing.RIGHT
        }

        it("with both buttons pressed during a shot, the left side does not set the facing") {
            slashOnGround()
            megaman.facing = Facing.RIGHT

            runWithBothPressed(Facing.LEFT)

            megaman.facing shouldBe Facing.RIGHT
        }
    }
})
