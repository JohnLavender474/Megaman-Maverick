package com.megaman.maverick.game.entities.protoman

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.Points
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.enemies.MetaWheelF
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.testServices
import com.maverick.game.engine.components.require
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.unmockkAll

private const val DELTA = 0.05f
private const val TOLERANCE = 0.001f

class ProtomanTest : DescribeSpec({

    lateinit var proto: Protoman
    lateinit var body: Body
    lateinit var controllerSystem: ControllerSystem
    var frame = InputFrame.RELEASED
    var overGround = true

    val impulse = ProtomanValues.RUN_IMPULSE * DELTA * ConstVals.PPM

    fun step(vararg buttons: Pair<MegaControllerButton, ButtonStatus>) {
        frame = InputFrame.RELEASED
        buttons.forEach { (button, status) -> frame = frame.with(button, status) }
        controllerSystem.update(DELTA)
    }

    fun hold(button: MegaControllerButton, seconds: Float) {
        step(button to ButtonStatus.JUST_PRESSED)
        var time = 0f
        while (time < seconds - TOLERANCE) {
            step(button to ButtonStatus.PRESSED)
            time += DELTA
        }
    }

    fun hurt() {
        val damager = MetaWheelF(proto.services).also { it.addComponent(TagsComponent(MetaWheelF.TAG)) }
        proto.takeDamageFrom(damager) shouldBe true
    }

    beforeEach {
        frame = InputFrame.RELEASED
        overGround = true
        val services = testServices(engine = mockk<GameEngine>(relaxed = true))
        services.levelState.currentLevel = LevelDefinition.REFACTOR_TEST_060
        MegaEntityFactory.init(services)
        proto = Protoman(services)
        proto.existenceState = EntityExistenceState.LIVE
        proto.facing = Facing.RIGHT
        proto.addComponent(proto.defineBodyComponent())
        body = (proto.getComponent(BodyComponent::class)!!).body
        body.putProperty(ConstKeys.BODY_OVER_GROUND, { overGround })
        proto.direction = Direction.UP
        services.players.register(PlayerSlot(0), proto)
        proto.addComponent(proto.defineControllerComponent().also { it.input = InputSource { frame } })
        proto.addComponent(RunComponent())
        proto.addComponent(LocksComponent(MegaLockGroup.entries.size))
        proto.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        proto.addComponent(TeleportableComponent())
        proto.addComponent(proto.damageRecovery)
        proto.addComponent(proto.defineStunComponent())
        proto.addComponent(proto.defineFreezeComponent())
        proto.addComponent(proto.defineMomentumComponent())
        proto.addComponent(proto.defineBehaviorsComponent())
        proto.addComponent(AudioComponent())
        proto.addComponent(
            PointsComponent(ConstKeys.HEALTH pairTo Points(0, ConstVals.MAX_HEALTH, ConstVals.MAX_HEALTH))
        )

        controllerSystem = ControllerSystem()
        controllerSystem.add(proto)
    }

    afterEach {
        MegaEntityFactory.reset()
        unmockkAll()
    }

    describe("the run") {
        it("adds the run impulse to the right on each held step") {
            step(MegaControllerButton.RIGHT to ButtonStatus.JUST_PRESSED)
            step(MegaControllerButton.RIGHT to ButtonStatus.PRESSED)
            body.physics.velocity.x shouldBe (impulse plusOrMinus TOLERANCE)
            proto.running shouldBe true
            proto.facing shouldBe Facing.RIGHT
        }

        it("faces and runs left") {
            step(MegaControllerButton.LEFT to ButtonStatus.JUST_PRESSED)
            step(MegaControllerButton.LEFT to ButtonStatus.PRESSED)
            proto.facing shouldBe Facing.LEFT
            body.physics.velocity.x shouldBe (-impulse plusOrMinus TOLERANCE)
        }

        it("stops adding speed at the max run speed") {
            hold(MegaControllerButton.RIGHT, 1f)
            body.physics.velocity.x shouldBeGreaterThanOrEqual ProtomanValues.RUN_MAX_SPEED * ConstVals.PPM
            body.physics.velocity.x shouldBeLessThan ProtomanValues.RUN_MAX_SPEED * ConstVals.PPM + impulse
        }

        it("stops running on the release") {
            hold(MegaControllerButton.RIGHT, 0.3f)
            step(MegaControllerButton.RIGHT to ButtonStatus.JUST_RELEASED)
            proto.running shouldBe false
        }

        it("does not run while damaged") {
            hurt()
            hold(MegaControllerButton.RIGHT, 0.2f)
            body.physics.velocity.x shouldBe 0f
            proto.running shouldBe false
        }
    }

    describe("the jump") {
        fun jump() = proto.defineBehaviorsComponent().getBehavior(BehaviorType.JUMPING)!!

        it("starts on a new press of A over the ground with the jump speed") {
            val jump = jump()
            frame = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)
            jump.evaluate(0f) shouldBe true
            jump.init()
            body.physics.velocity.y shouldBe (ProtomanValues.JUMP_VEL * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("jumps down when the gravity points down") {
            proto.direction = Direction.DOWN
            jump().init()
            body.physics.velocity.y shouldBe (-ProtomanValues.JUMP_VEL * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("jumps while DOWN is held") {
            frame = InputFrame.RELEASED
                .with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)
                .with(MegaControllerButton.DOWN, ButtonStatus.PRESSED)
            jump().evaluate(0f) shouldBe true
        }

        it("does not start in the air") {
            overGround = false
            frame = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)
            jump().evaluate(0f) shouldBe false
        }

        it("does not start on a held A") {
            frame = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.PRESSED)
            jump().evaluate(0f) shouldBe false
        }
    }

    describe("the gravity") {
        it("uses the fall gravity in the air") {
            overGround = false
            body.preProcess()
            body.physics.gravity.y shouldBe (ProtomanValues.FALL_GRAVITY * ConstVals.PPM plusOrMinus TOLERANCE)
        }

        it("signs the gravity by the direction") {
            overGround = false
            proto.direction = Direction.DOWN
            body.preProcess()
            body.physics.gravity.y shouldBe (-ProtomanValues.FALL_GRAVITY * ConstVals.PPM plusOrMinus TOLERANCE)
        }
    }

    describe("the animation key") {
        val animations = ObjectMap<String, IAnimation>()
        animations.put("run_trans", Animation(gdxArrayOf(TextureRegion()), ProtomanValues.STEP_TIME, false))

        it("shows the step when he stops, then stands") {
            animations["run_trans"].reset()
            proto.animationKey("run", animations) shouldBe "run_trans"
            proto.animationKey("run_trans", animations) shouldBe "run_trans"
            animations["run_trans"].update(ProtomanValues.STEP_TIME)
            proto.animationKey("run_trans", animations) shouldBe "stand"
            proto.animationKey("stand", animations) shouldBe "stand"
        }

        it("shows the step at the spawn and on a landing") {
            animations["run_trans"].reset()
            proto.animationKey(null, animations) shouldBe "run_trans"
            proto.animationKey("jump", animations) shouldBe "run_trans"
        }

        it("runs and jumps") {
            hold(MegaControllerButton.RIGHT, 0.1f)
            proto.animationKey("stand", animations) shouldBe "run"
            overGround = false
            proto.animationKey("run", animations) shouldBe "jump"
        }

        it("shows the damage key while damaged") {
            hurt()
            proto.animationKey("stand", animations) shouldBe "damaged"
        }
    }

    describe("the sprite") {
        fun drawn(): GameSprite {
            val sprites = proto.defineSpritesComponent()
            sprites.preProcess(0f)
            return sprites.sprites[Protoman.TAG]!!
        }

        it("shows in play") {
            val sprite = drawn()
            sprite.hidden shouldBe false
            sprite.color.a shouldBe 1f
        }

        it("hides out of play") {
            proto.setPlayPhase(PlayPhase.PRE_PLAY)
            drawn().hidden shouldBe true
        }

        it("shows while frozen, also out of play") {
            proto.require(FreezeComponent::class).freeze()
            proto.setPlayPhase(PlayPhase.PRE_PLAY)
            drawn().hidden shouldBe false
        }

        it("flashes in the damage recovery") {
            proto.damageRecovery.update(MegamanValues.DAMAGE_FLASH_DURATION, false, true)
            drawn().color.a shouldBe 0f
        }
    }
})
