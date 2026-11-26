package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.controller.ControllerSystem
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.handlers.MegamanWeaponsHandler
import com.megaman.maverick.game.players.PlayerLight
import com.megaman.maverick.game.players.lightOrNull
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

class MegamanChargeLightTest : DescribeSpec({

    lateinit var rig: MegamanSpawnedRig
    lateinit var controllerSystem: ControllerSystem

    fun hold(delta: Float) {
        rig.press(MegaControllerButton.B to ButtonStatus.PRESSED)
        controllerSystem.update(delta)
    }

    beforeEach {
        rig = MegamanSpawnedRig()
        val weapons = mockk<MegamanWeaponsHandler>(relaxed = true)
        every { weapons.canFireWeapon(any(), any()) } returns true
        rig.megaman.weaponsHandler = weapons
        controllerSystem = ControllerSystem()
        controllerSystem.add(rig.megaman)
    }

    afterEach { unmockkAll() }

    describe("the light of Mega Man's charge") {
        it("is the standard light below the half time") {
            hold(MegamanValues.TIME_TO_HALFWAY_CHARGED - 0.01f)

            rig.megaman.lightOrNull shouldBe PlayerLight.STANDARD
        }

        it("is the half charge light from the half time") {
            hold(MegamanValues.TIME_TO_HALFWAY_CHARGED)

            rig.megaman.lightOrNull shouldBe PlayerLight.HALF_CHARGE
        }

        it("is the full charge light at the full time") {
            hold(MegamanValues.TIME_TO_FULLY_CHARGED)

            rig.megaman.lightOrNull shouldBe PlayerLight.FULL_CHARGE
        }
    }
})
