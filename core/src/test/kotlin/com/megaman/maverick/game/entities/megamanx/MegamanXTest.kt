package com.megaman.maverick.game.entities.megamanx

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyKeys
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.addBodyLabel
import com.megaman.maverick.game.world.body.receiver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import com.maverick.game.engine.common.enums.Direction

private const val DELTA = 0.05f
private const val TOLERANCE = 0.001f

private fun MegamanX.damageTimerForTest(): Timer {
    val field = MegamanX::class.java.getDeclaredField("damageTimer").apply { isAccessible = true }
    return field.get(this) as Timer
}

private fun MegamanX.startDamage() = damageTimerForTest().reset()

class MegamanXTest : DescribeSpec({

    lateinit var x: MegamanX
    lateinit var body: Body
    lateinit var controllerSystem: ControllerSystem
    lateinit var spawned: MutableList<Pair<IGameEntity, Properties>>
    var frame = InputFrame.RELEASED
    var overGround = true

    fun step(vararg buttons: Pair<MegaControllerButton, ButtonStatus>, delta: Float = DELTA) {
        frame = InputFrame.RELEASED
        buttons.forEach { (button, status) -> frame = frame.with(button, status) }
        controllerSystem.update(delta)
        x.buster.update(delta)
    }

    fun hold(button: MegaControllerButton, seconds: Float) {
        step(button to ButtonStatus.JUST_PRESSED)
        var time = 0f
        while (time < seconds - TOLERANCE) {
            step(button to ButtonStatus.PRESSED)
            time += DELTA
        }
    }

    fun press(button: MegaControllerButton) {
        step(button to ButtonStatus.JUST_PRESSED)
        step(button to ButtonStatus.PRESSED)
    }

    fun tapB() {
        step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
        step(MegaControllerButton.B to ButtonStatus.JUST_RELEASED)
        x.buster.update(1f)
    }

    beforeEach {
        frame = InputFrame.RELEASED
        overGround = true
        spawned = mutableListOf()

        val engine = mockk<GameEngine>(relaxed = true)
        every { engine.spawn(any(), any()) } answers {
            val entity = firstArg<IGameEntity>()
            val props = secondArg<Properties>()
            entity.existenceState = EntityExistenceState.LIVE
            @Suppress("UNCHECKED_CAST")
            (entity as IOwnable<IGameEntity>).owner = props.get(ConstKeys.OWNER) as IGameEntity?
            spawned.add(entity to props)
            true
        }
        val services = testServices(engine = engine)
        services.levelState.currentLevel = LevelDefinition.X6_INTRO
        MegaEntityFactory.init(services)

        x = MegamanX(services)
        x.existenceState = EntityExistenceState.LIVE
        x.facing = Facing.RIGHT

        x.addComponent(x.defineBodyComponent())
        body = (x.getComponent(BodyComponent::class)!!).body
        body.putProperty(ConstKeys.BODY_OVER_GROUND, { overGround })
        x.addComponent(x.defineControllerComponent().also { it.input = InputSource { frame } })
        x.addComponent(RunComponent())
        x.addComponent(LocksComponent(MegaLockGroup.entries.size))
        x.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        x.addComponent(TeleportableComponent())
        x.addComponent(x.damageRecovery)
        x.addComponent(x.defineStunComponent())
        x.addComponent(x.defineFreezeComponent())
        x.addComponent(x.defineWeaponChargeComponent())
        x.addComponent(PostActionMomentumComponent(0f, 0f, { 0f }, { true }, { false }, { false }))
        x.addComponent(BehaviorsComponent())
        x.addComponent(AudioComponent())

        controllerSystem = ControllerSystem()
        controllerSystem.add(x)
    }

    afterEach {
        MegaEntityFactory.reset()
        unmockkAll()
    }

    describe("the run") {
        it("walks at the walk start speed, then at the walk speed") {
            press(MegaControllerButton.RIGHT)
            body.physics.velocity.x shouldBe
                (MegamanXValues.WALK_START_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)

            hold(MegaControllerButton.RIGHT, MegamanXValues.WALK_START_TIME + DELTA)
            step(MegaControllerButton.RIGHT to ButtonStatus.PRESSED)
            body.physics.velocity.x shouldBe (MegamanXValues.WALK_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("walks at the walk speed at once in the air") {
            overGround = false
            press(MegaControllerButton.RIGHT)
            body.physics.velocity.x shouldBe (MegamanXValues.WALK_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("faces and walks left") {
            overGround = false
            press(MegaControllerButton.LEFT)
            x.facing shouldBe Facing.LEFT
            body.physics.velocity.x shouldBe (-MegamanXValues.WALK_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("stops at once on the release") {
            hold(MegaControllerButton.RIGHT, 0.3f)
            step(MegaControllerButton.RIGHT to ButtonStatus.JUST_RELEASED)
            body.preProcess()
            x.running shouldBe false
            body.physics.velocity.x shouldBe 0f
        }

        it("does not walk while damaged") {
            x.startDamage()
            press(MegaControllerButton.RIGHT)
            body.physics.velocity.x shouldBe 0f
        }

        it("drags the side speed only while damaged") {
            x.startDamage()
            body.preProcess()
            body.physics.applyFrictionX shouldBe true
            body.physics.defaultFrictionOnSelf.x shouldBe MegamanXValues.KNOCKBACK_DRAG

            x.damageTimerForTest().setToEnd()
            body.preProcess()
            body.physics.applyFrictionX shouldBe false
            body.physics.applyFrictionY shouldBe false
        }

        it("drags on the side axis of a body turned left") {
            body.direction = Direction.LEFT
            x.startDamage()
            body.preProcess()
            body.physics.applyFrictionX shouldBe false
            body.physics.applyFrictionY shouldBe true
            body.physics.defaultFrictionOnSelf.y shouldBe MegamanXValues.KNOCKBACK_DRAG
        }

        it("keeps the knockback speed while damaged") {
            x.startDamage()
            body.physics.velocity.x = -5f
            body.preProcess()
            body.physics.velocity.x shouldBe -5f
        }
    }

    describe("the body") {
        it("uses the X6 gravity and fall limit in the air") {
            overGround = false
            body.preProcess()
            body.physics.gravity.y shouldBe (MegamanXValues.GRAVITY * ConstVals.PPM plusOrMinus TOLERANCE)
            body.physics.velocityClamp.y shouldBe (MegamanXValues.FALL_MAX * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("uses the ground gravity over the ground") {
            body.preProcess()
            body.physics.gravity.y shouldBe (MegamanXValues.GROUND_GRAVITY * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("has no air drag") {
            body.physics.applyFrictionX shouldBe false
            body.physics.applyFrictionY shouldBe false
        }

        it("does not move before play") {
            x.setPlayPhase(PlayPhase.PRE_PLAY)
            body.physics.velocity.set(3f, 3f)
            body.preProcess()
            body.physics.velocity.x shouldBe 0f
            body.physics.velocity.y shouldBe 0f
        }

        it("does not count a one-way top as ground while DOWN is held") {
            val realBody = x.defineBodyComponent().body
            val feetGravity = realBody.getProperty("${ConstKeys.FEET}_${ConstKeys.GRAVITY}") as Fixture
            val consume = feetGravity.receiver(Receivers.CONSUMER)!!
            val overGroundNow = realBody.getProperty(ConstKeys.BODY_OVER_GROUND) as () -> Boolean
            val blockBody = Body(BodyType.STATIC)
            blockBody.addBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)
            val block = mockk<Block>(relaxed = true)
            every { block.body } returns blockBody
            val blockFixture = Fixture(blockBody, FixtureType.BLOCK, GameRectangle())
            blockFixture.putProperty(ConstKeys.ENTITY, block)

            consume(ProcessState.BEGIN, blockFixture)
            overGroundNow() shouldBe true

            frame = InputFrame.RELEASED.with(MegaControllerButton.DOWN, ButtonStatus.PRESSED)
            consume(ProcessState.CONTINUE, blockFixture)
            overGroundNow() shouldBe false
        }

        it("passes a one-way top only while DOWN is held") {
            val passes = body.getProperty(BodyKeys.PASS_THROUGH_ONE_WAY) as () -> Boolean
            passes() shouldBe false
            frame = InputFrame.RELEASED.with(MegaControllerButton.DOWN, ButtonStatus.PRESSED)
            passes() shouldBe true
        }
    }

    describe("the jump") {
        fun jumpBehavior() = x.defineBehaviorsComponent().getBehavior(BehaviorType.JUMPING)!!

        it("starts on a new press of A over the ground with the X6 jump speed") {
            val jump = jumpBehavior()
            frame = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)
            jump.evaluate(0f) shouldBe true
            jump.init()
            body.physics.velocity.y shouldBe (MegamanXValues.JUMP_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("does not take Megaman's jump boost") {
            x.services.gameState.addEnhancement(MegaEnhancement.JUMP_BOOST)
            jumpBehavior().init()
            body.physics.velocity.y shouldBe (MegamanXValues.JUMP_SPEED * ConstVals.PPM plusOrMinus TOLERANCE)
        }
    }

    describe("the buster") {
        it("shoots a bullet on the press") {
            step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)

            spawned.size shouldBe 1
            spawned[0].first.shouldBeInstanceOf<Bullet>()
            spawned[0].second.get(ConstKeys.OWNER) shouldBe x
            x.buster.shooting shouldBe true
        }

        it("waits before the next shot") {
            step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED, delta = 0.01f)
            step(MegaControllerButton.B to ButtonStatus.JUST_RELEASED, delta = 0.01f)
            step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED, delta = 0.01f)

            spawned.size shouldBe 1
        }

        it("has at most 3 shots on the screen") {
            repeat(4) { tapB() }

            spawned.size shouldBe MegamanXValues.SHOT_LIMIT
        }

        it("shoots again when a shot is gone") {
            repeat(3) { tapB() }
            spawned[0].first.existenceState = EntityExistenceState.UNSPAWNED
            tapB()

            spawned.size shouldBe 4
        }

        it("shoots nothing more on a release with no charge") {
            step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            x.buster.update(1f)
            step(MegaControllerButton.B to ButtonStatus.JUST_RELEASED)

            spawned.size shouldBe 1
        }

        it("shoots a half charged shot on the release after the half charge time") {
            hold(MegaControllerButton.B, MegamanXValues.TIME_TO_HALF_CHARGE + DELTA)
            x.chargeStatus shouldBe MegaChargeStatus.HALF_CHARGED
            step(MegaControllerButton.B to ButtonStatus.JUST_RELEASED)

            spawned.size shouldBe 2
            spawned[1].first.shouldBeInstanceOf<ChargedShot>()
            spawned[1].second.get(ConstKeys.BOOLEAN) shouldBe false
        }

        it("shoots a full charged shot on the release after the full charge time") {
            hold(MegaControllerButton.B, MegamanXValues.TIME_TO_FULL_CHARGE + DELTA)
            step(MegaControllerButton.B to ButtonStatus.JUST_RELEASED)

            spawned.size shouldBe 2
            spawned[1].first.shouldBeInstanceOf<ChargedShot>()
            spawned[1].second.get(ConstKeys.BOOLEAN) shouldBe true
        }

        it("has at most 2 shots on the screen for a full charged shot") {
            repeat(2) { tapB() }
            hold(MegaControllerButton.B, MegamanXValues.TIME_TO_FULL_CHARGE + DELTA)
            step(MegaControllerButton.B to ButtonStatus.JUST_RELEASED)

            spawned.none { it.first is ChargedShot } shouldBe true
        }

        it("drops the charge when damaged") {
            hold(MegaControllerButton.B, 1f)
            x.startDamage()
            step(MegaControllerButton.B to ButtonStatus.PRESSED)
            x.chargeStatus shouldBe MegaChargeStatus.NOT_CHARGED
        }
    }

    describe("the animation key") {
        it("stands") {
            x.animationKey() shouldBe "stand"
        }

        it("shows the walk start, then the walk") {
            press(MegaControllerButton.RIGHT)
            x.animationKey() shouldBe "run_trans"
            hold(MegaControllerButton.RIGHT, MegamanXValues.WALK_START_TIME + DELTA)
            x.animationKey() shouldBe "run"
        }

        it("rises and falls in the air") {
            overGround = false
            body.physics.velocity.y = 1f
            x.animationKey() shouldBe "jump"
            body.physics.velocity.y = -1f
            x.animationKey() shouldBe "fall"
        }

        it("uses the shoot poses while the shot pose time runs") {
            step(MegaControllerButton.B to ButtonStatus.JUST_PRESSED)
            x.animationKey() shouldBe "land_shoot"
            overGround = false
            x.animationKey() shouldBe "fall_shoot"
        }

        it("shows the damage pose") {
            x.startDamage()
            x.animationKey() shouldBe "damaged"
        }
    }
})
