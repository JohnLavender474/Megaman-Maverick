package com.megaman.maverick.game.entities.megaman.weapons

import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.handlers.MegamanRushHandler
import com.megaman.maverick.game.weapons.WeaponDef
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class MegamanWeaponRulesTest : DescribeSpec({

    fun megamanWith(weapon: WeaponDef<Megaman>, joined: Boolean): Megaman {
        val rushHandler = mockk<MegamanRushHandler>()
        every { rushHandler.joined } returns joined
        val megaman = mockk<Megaman>()
        every { megaman.currentWeapon } returns weapon
        every { megaman.rushHandler } returns rushHandler
        return megaman
    }

    describe("a Rush ability before Rush joins") {
        it("draws the buster and gives no jet moves") {
            val megaman = megamanWith(MegamanWeapons.RUSH_JET, joined = false)
            megaman.weaponMovesReady shouldBe false
            megaman.drawnWeapon shouldBe MegamanWeapons.MEGA_BUSTER
            megaman.jetpackEnabled shouldBe false
            megaman.weaponAirDash shouldBe null
        }
    }

    describe("a Rush ability with Rush joined") {
        it("draws the ability and gives its moves") {
            val megaman = megamanWith(MegamanWeapons.RUSH_JET, joined = true)
            megaman.weaponMovesReady shouldBe true
            megaman.drawnWeapon shouldBe MegamanWeapons.RUSH_JET
            megaman.jetpackEnabled shouldBe true
            megaman.weaponAirDash shouldBe MegamanWeapons.RUSH_JET.airDash
        }
    }

    describe("a weapon that is not a Rush ability") {
        it("does not need Rush") {
            val megaman = megamanWith(MegamanWeapons.MOON_SCYTHES, joined = false)
            megaman.weaponMovesReady shouldBe true
            megaman.drawnWeapon shouldBe MegamanWeapons.MOON_SCYTHES
        }
    }
})
