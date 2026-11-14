package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.components.chargeStatus
import com.megaman.maverick.game.entities.megaman.components.charging
import com.megaman.maverick.game.entities.megaman.components.clearChargeWhenHurt
import com.megaman.maverick.game.entities.megaman.components.defineWeaponChargeComponent
import com.megaman.maverick.game.entities.megaman.components.fullyCharged
import com.megaman.maverick.game.entities.megaman.components.halfCharged
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.megaman.handlers.MegamanWeaponsHandler
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.StunComponent
import com.megaman.maverick.game.entities.state.TeleportableComponent
import com.megaman.maverick.game.entities.state.WeaponChargeComponent
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import com.megaman.maverick.game.players.stopCharging

private const val HALF = MegamanValues.TIME_TO_HALFWAY_CHARGED
private const val FULL = MegamanValues.TIME_TO_FULLY_CHARGED

class MegamanWeaponChargeTest : DescribeSpec({

    lateinit var megaman: Megaman
    lateinit var services: MegaServices
    lateinit var weapons: MegamanWeaponsHandler
    lateinit var audio: AudioComponent
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

    fun chargeSounds() = audio.playSoundRequests.count { it.source == SoundAsset.MEGA_BUSTER_CHARGING_SOUND }

    fun chargeSoundStops() = audio.stopSoundRequests.count { it == SoundAsset.MEGA_BUSTER_CHARGING_SOUND }

    beforeEach {
        frame = InputFrame.RELEASED

        services = testServices(engine = mockk<GameEngine>(relaxed = true))
        services.levelState.currentLevel = LevelDefinition.DESERT_MAN

        weapons = mockk(relaxed = true)
        every { weapons.isChargeable(any()) } returns true
        every { weapons.canFireWeapon(any(), any()) } returns true

        megaman = Megaman(services)
        megaman.weaponsHandler = weapons
        megaman.currentWeapon = MegamanWeapon.MEGA_BUSTER
        megaman.facing = Facing.RIGHT

        audio = AudioComponent()
        megaman.addComponent(audio)
        megaman.addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
        megaman.addComponent(megaman.defineControllerComponent().also { it.input = InputSource { frame } })
        megaman.addComponent(megaman.defineMomentumComponent())
        megaman.addComponent(LocksComponent(MegaLockGroup.entries.size))
        megaman.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        megaman.addComponent(TeleportableComponent())
        megaman.addComponent(RunComponent())
        megaman.addComponent(megaman.defineWeaponChargeComponent())
        megaman.addComponent(BehaviorsComponent())
        megaman.addComponent(megaman.defineStunComponent())

        controllerSystem = ControllerSystem()
        controllerSystem.add(megaman)
    }

    afterEach { unmockkAll() }

    describe("the charge status") {
        it("is not charged before the first update") {
            megaman.chargeStatus shouldBe MegaChargeStatus.NOT_CHARGED
            megaman.charging shouldBe false
        }

        it("is not charged after the component reset of a destroy") {
            hold(FULL)

            megaman.getComponent(WeaponChargeComponent::class)!!.reset()

            megaman.chargeStatus shouldBe MegaChargeStatus.NOT_CHARGED
            megaman.charging shouldBe false
        }

        it("is not charged below the half time") {
            hold(HALF - 0.01f)

            megaman.chargeStatus shouldBe MegaChargeStatus.NOT_CHARGED
            megaman.charging shouldBe false
            megaman.halfCharged shouldBe false
            megaman.fullyCharged shouldBe false
        }

        it("is half charged from the half time") {
            hold(HALF)

            megaman.chargeStatus shouldBe MegaChargeStatus.HALF_CHARGED
            megaman.charging shouldBe true
            megaman.halfCharged shouldBe true
            megaman.fullyCharged shouldBe false
        }

        it("is fully charged at the full time") {
            hold(FULL)

            megaman.chargeStatus shouldBe MegaChargeStatus.FULLY_CHARGED
            megaman.charging shouldBe true
            megaman.halfCharged shouldBe false
            megaman.fullyCharged shouldBe true
        }

        it("is not charged with a weapon that does not charge") {
            hold(FULL)
            every { weapons.isChargeable(any()) } returns false

            megaman.chargeStatus shouldBe MegaChargeStatus.NOT_CHARGED
            megaman.charging shouldBe false
            megaman.fullyCharged shouldBe false
        }
    }

    describe("the attack button") {
        it("fires with the charge status and clears the charge") {
            hold(HALF)

            release()

            verify { weapons.fireWeapon(MegamanWeapon.MEGA_BUSTER, MegaChargeStatus.HALF_CHARGED) }
            megaman.chargeStatus shouldBe MegaChargeStatus.NOT_CHARGED
            chargeSoundStops() shouldBe 1
        }

        it("clears the charge when the weapon cannot fire the next status") {
            hold(HALF)
            every { weapons.canFireWeapon(any(), MegaChargeStatus.FULLY_CHARGED) } returns false

            hold(0.01f)

            megaman.chargeStatus shouldBe MegaChargeStatus.NOT_CHARGED
            chargeSoundStops() shouldBe 1
        }

        it("charges faster with the enhancement") {
            services.gameState.addEnhancement(MegaEnhancement.FASTER_BUSTER_CHARGING)

            hold(HALF / MegaEnhancement.FASTER_BUSTER_CHARGING_SCALAR)

            megaman.chargeStatus shouldBe MegaChargeStatus.HALF_CHARGED
        }
    }

    describe("the charge sound") {
        it("plays once at the half time") {
            hold(HALF - 0.01f)
            chargeSounds() shouldBe 0

            hold(0.01f)
            hold(0.1f)

            chargeSounds() shouldBe 1
        }

        it("plays again on the next charge") {
            hold(HALF)
            megaman.stopCharging()

            hold(HALF)

            chargeSounds() shouldBe 2
        }
    }

    describe("stopCharging") {
        it("clears the charge and stops the sound") {
            hold(FULL)

            megaman.stopCharging()

            megaman.chargeStatus shouldBe MegaChargeStatus.NOT_CHARGED
            chargeSoundStops() shouldBe 1
        }
    }

    describe("clearChargeWhenHurt") {
        it("keeps the charge when Mega is not hurt") {
            hold(HALF)

            megaman.clearChargeWhenHurt()

            megaman.chargeStatus shouldBe MegaChargeStatus.HALF_CHARGED
        }

        it("clears the charge during a stun, with no sound stop") {
            hold(HALF)
            megaman.getComponent(StunComponent::class)!!.start()

            megaman.clearChargeWhenHurt()

            megaman.chargeStatus shouldBe MegaChargeStatus.NOT_CHARGED
            chargeSoundStops() shouldBe 0
        }
    }
})
