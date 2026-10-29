package com.megaman.maverick.game.entities.megaman.handlers

import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.state.GameState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

class MegamanWeaponsHandlerOwnershipTest : DescribeSpec({

    lateinit var state: GameState
    lateinit var handler: MegamanWeaponsHandler

    beforeEach {
        state = GameState()
        val game = testServices(state = state)
        val megaman = mockk<Megaman>(relaxed = true)
        every { megaman.game } returns game
        handler = MegamanWeaponsHandler(megaman)
    }

    afterEach { unmockkAll() }

    describe("hasWeapon") {
        it("reads the owned weapons in the game state") {
            handler.hasWeapon(MegamanWeapon.MEGA_BUSTER) shouldBe true
            handler.hasWeapon(MegamanWeapon.FRIGID_SHOT) shouldBe false

            state.addLevelDefeated(LevelDefinition.GLACIER_MAN)
            handler.hasWeapon(MegamanWeapon.FRIGID_SHOT) shouldBe true

            state.removeLevelDefeated(LevelDefinition.GLACIER_MAN)
            handler.hasWeapon(MegamanWeapon.FRIGID_SHOT) shouldBe false
        }

        it("gives a gained weapon full ammo") {
            state.addWeapon(MegamanWeapon.AXE_SWINGER)
            handler.getAmmo(MegamanWeapon.AXE_SWINGER) shouldBe MegamanValues.MAX_WEAPON_AMMO
        }

        it("gives no ammo for a weapon that is not owned") {
            handler.getAmmo(MegamanWeapon.AXE_SWINGER) shouldBe 0
        }
    }
})
