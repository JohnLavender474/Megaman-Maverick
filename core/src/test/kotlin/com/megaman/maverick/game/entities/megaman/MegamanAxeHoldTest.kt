package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.behaviors.climbBehavior
import com.megaman.maverick.game.entities.megaman.components.clearChargeWhenHurt
import com.megaman.maverick.game.entities.megaman.components.holdingAxe
import com.megaman.maverick.game.entities.megaman.components.onChangeAnimationKey
import com.megaman.maverick.game.entities.megaman.components.shouldUpdateAnimation
import com.megaman.maverick.game.entities.megaman.components.stopWindupNotOfCurrentWeapon
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.handlers.MegamanWeaponsHandler
import com.megaman.maverick.game.entities.megaman.sprites.amendKey
import com.megaman.maverick.game.entities.megaman.sprites.getAnimationKey
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.StunComponent
import com.megaman.maverick.game.entities.state.TeleportableComponent
import com.megaman.maverick.game.entities.state.WeaponWindupComponent
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.players.windups
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify

private const val AXE_KEY = "stand_axe_throw_axeswinger"

class MegamanAxeHoldTest : DescribeSpec({

    lateinit var megaman: Megaman
    lateinit var services: MegaServices
    lateinit var weapons: MegamanWeaponsHandler
    lateinit var controllerSystem: ControllerSystem
    var frame = InputFrame.RELEASED

    fun hold(delta: Float) {
        frame = InputFrame.RELEASED.with(MegaControllerButton.B, ButtonStatus.PRESSED)
        controllerSystem.update(delta)
    }

    fun release() {
        frame = InputFrame.RELEASED.with(MegaControllerButton.B, ButtonStatus.JUST_RELEASED)
        controllerSystem.update(0.01f)
    }

    beforeEach {
        frame = InputFrame.RELEASED

        services = testServices(engine = mockk<GameEngine>(relaxed = true))
        services.levelState.currentLevel = LevelDefinition.DESERT_MAN

        weapons = mockk(relaxed = true)
        every { weapons.canFireWeapon(any(), any()) } returns true

        megaman = Megaman(services)
        megaman.weaponsHandler = weapons
        megaman.currentWeapon = MegamanWeapons.AXE_SWINGER
        megaman.facing = Facing.RIGHT

        megaman.addComponent(AudioComponent())
        megaman.addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
        megaman.addComponent(megaman.defineControllerComponent().also { it.input = InputSource { frame } })
        megaman.addComponent(megaman.defineMomentumComponent())
        megaman.addComponent(LocksComponent(MegaLockGroup.entries.size))
        megaman.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        megaman.addComponent(TeleportableComponent())
        megaman.addComponent(RunComponent())
        megaman.addComponent(WeaponWindupComponent())
        megaman.addComponent(BehaviorsComponent())
        megaman.addComponent(megaman.defineStunComponent())

        controllerSystem = ControllerSystem()
        controllerSystem.add(megaman)
    }

    afterEach { unmockkAll() }

    describe("the hold of the axe") {
        it("advances the axe windup while the attack button is held") {
            hold(0.2f)
            hold(0.1f)

            megaman.axeWindup.time shouldBe (0.3f plusOrMinus 0.001f)
            megaman.windups.active shouldBe megaman.axeWindup
            megaman.holdingAxe shouldBe true
        }

        it("does not set shooting") {
            hold(0.3f)

            megaman.shooting shouldBe false
        }

        it("gates on the fire of an uncharged throw") {
            every { weapons.canFireWeapon(MegamanWeapons.AXE_SWINGER, MegaChargeStatus.NOT_CHARGED) } returns false

            hold(0.3f)

            megaman.axeWindup.time shouldBe 0f
            megaman.holdingAxe shouldBe false
        }

        it("starts when the gate first passes, as at the end of the cooldown") {
            every { weapons.canFireWeapon(MegamanWeapons.AXE_SWINGER, MegaChargeStatus.NOT_CHARGED) } returns false
            hold(0.3f)
            every { weapons.canFireWeapon(MegamanWeapons.AXE_SWINGER, MegaChargeStatus.NOT_CHARGED) } returns true

            hold(0.1f)

            megaman.axeWindup.time shouldBe (0.1f plusOrMinus 0.001f)
        }

        it("pauses, and does not clear, during a room transition") {
            hold(0.2f)
            services.levelState.roomTransition = true

            hold(0.2f)

            megaman.axeWindup.time shouldBe (0.2f plusOrMinus 0.001f)
            megaman.holdingAxe shouldBe true
        }

        it("clears on a stun") {
            hold(0.2f)
            megaman.getComponent(StunComponent::class)!!.start()

            megaman.clearChargeWhenHurt()

            megaman.axeWindup.time shouldBe 0f
            megaman.holdingAxe shouldBe false
        }

        it("clears on a switch to another weapon") {
            hold(0.2f)
            megaman.currentWeapon = MegamanWeapons.MEGA_BUSTER

            megaman.stopWindupNotOfCurrentWeapon()

            megaman.axeWindup.time shouldBe 0f
            megaman.windups.active shouldBe null
        }

        it("fires the axe uncharged on release, and clears the windup") {
            hold(0.3f)

            release()

            verify { weapons.fireWeapon(MegamanWeapons.AXE_SWINGER, MegaChargeStatus.NOT_CHARGED) }
            megaman.axeWindup.time shouldBe 0f
            megaman.holdingAxe shouldBe false
        }
    }

    describe("the pose of a hold") {
        it("gives the axe throw key of each base that has one") {
            hold(0.2f)

            listOf("stand", "run", "jump", "swim", "wallslide", "groundslide", "airdash", "crouch", "slip", "climb_still")
                .forEach { base -> megaman.amendKey(base) shouldBe "${base}_axe_throw" }
        }

        it("gives the base key with no hold") {
            megaman.amendKey("stand") shouldBe "stand"
        }
    }

    describe("the freeze of the animation") {
        it("does not advance an axe throw key while holding, and keeps it at its first frame") {
            hold(0.2f)
            val animation = mockk<IAnimation>(relaxed = true)

            megaman.shouldUpdateAnimation(AXE_KEY, animation) shouldBe false

            verify { animation.setCurrentTime(0f) }
        }

        it("shows the first frame of a real axe throw animation while holding, and the throw frame after the jump") {
            val animation = Animation(gdxArrayOf(TextureRegion(), TextureRegion()), 0.1f, loop = false)
            animation.update(0.15f)
            animation.reset()
            animation.getIndex() shouldBe 1
            hold(0.2f)

            megaman.shouldUpdateAnimation(AXE_KEY, animation) shouldBe false
            animation.getIndex() shouldBe 0

            animation.setCurrentTime(MegamanValues.AXE_WINDUP_TIME)
            animation.update(0.01f)
            animation.getIndex() shouldBe 1
        }

        it("advances an axe throw key with no hold") {
            val animation = mockk<IAnimation>(relaxed = true)

            megaman.shouldUpdateAnimation(AXE_KEY, animation) shouldBe true
        }

        it("advances another key while holding") {
            hold(0.2f)
            val animation = mockk<IAnimation>(relaxed = true)

            megaman.shouldUpdateAnimation("stand_megabuster", animation) shouldBe true
        }

        it("starts the next axe throw key at time 0 while holding") {
            hold(0.2f)
            val current = mockk<IAnimation>(relaxed = true)
            every { current.getCurrentTime() } returns 0.15f
            val next = mockk<IAnimation>(relaxed = true)
            val animations = OrderedMap<String, IAnimation>()
            animations.put(AXE_KEY, current)
            animations.put("run_axe_throw_axeswinger", next)

            megaman.onChangeAnimationKey(AXE_KEY, "run_axe_throw_axeswinger", animations)

            verify { next.setCurrentTime(0f) }
        }
    }
})

class MegamanAxeHoldClimbTest : DescribeSpec({

    val ppm = ConstVals.PPM.toFloat()

    lateinit var rig: MegamanMovesRig
    lateinit var climb: IBehavior

    beforeEach {
        rig = MegamanMovesRig()
        rig.megaman.shooting = false
        rig.megaman.currentWeapon = MegamanWeapons.AXE_SWINGER
        rig.megaman.addComponent(WeaponWindupComponent())
        rig.body.setSize(1f * ppm, 1.5f * ppm)
        val sensor = { GameRectangle().setSize(0.5f * ppm, 0.25f * ppm) }
        rig.body.addFixture(Fixture(rig.body, FixtureType.HEAD, sensor(), offsetFromBodyAttachment = Vector2(0f, 0.75f * ppm)))
        rig.body.addFixture(Fixture(rig.body, FixtureType.FEET, sensor(), offsetFromBodyAttachment = Vector2(0f, -0.75f * ppm)))

        val ladderBody = Body(BodyType.ABSTRACT).also { it.set(4f * ppm, 0f, 1f * ppm, 6f * ppm) }
        val ladder = mockk<IBodyEntity>(relaxed = true)
        every { ladder.body } returns ladderBody
        rig.body.putProperty(ConstKeys.LADDER, ladder)

        climb = rig.megaman.climbBehavior()
        rig.behaviors.addBehavior(BehaviorType.CLIMBING, climb)

        rig.body.setPosition(4.2f * ppm, 0f)
        rig.body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)
        rig.body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)
        rig.press(MegaControllerButton.UP to ButtonStatus.PRESSED)
        climb.update(0f)
    }

    afterEach { unmockkAll() }

    describe("a hold on a ladder") {
        it("holds still while UP is pressed") {
            rig.megaman.windups.hold(rig.megaman.axeWindup, 0.2f)

            climb.update(0f)

            climb.isActive() shouldBe true
            rig.body.physics.velocity.y shouldBe 0f
        }

        it("uses the still climb axe throw key") {
            rig.megaman.windups.hold(rig.megaman.axeWindup, 0.2f)
            climb.update(0f)

            rig.megaman.getAnimationKey(null) shouldBe "climb_still_axe_throw"
        }

        it("finishes the climb out of the ladder with no hold") {
            climb.update(0f)
            rig.body.setPosition(4f * ppm, 6.5f * ppm)

            rig.megaman.getAnimationKey(null) shouldBe "climb_finish"
        }

        it("uses the still climb axe throw key out of the ladder, where it would finish the climb") {
            rig.megaman.windups.hold(rig.megaman.axeWindup, 0.2f)
            climb.update(0f)
            rig.body.setPosition(4f * ppm, 6.5f * ppm)

            rig.megaman.getAnimationKey(null) shouldBe "climb_still_axe_throw"
        }
    }
})
