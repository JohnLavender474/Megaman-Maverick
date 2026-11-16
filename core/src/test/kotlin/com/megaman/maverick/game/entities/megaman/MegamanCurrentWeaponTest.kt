package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.GameEngine
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.weapons.WeaponDef
import com.megaman.maverick.game.entities.megaman.handlers.MegamanWeaponsHandler
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verifyOrder

class MegamanCurrentWeaponTest : DescribeSpec({

    lateinit var megaman: Megaman
    lateinit var handler: MegamanWeaponsHandler

    beforeEach {
        megaman = Megaman(testServices(engine = mockk<GameEngine>(relaxed = true)))
        handler = mockk(relaxed = true)
        megaman.weaponsHandler = handler
    }

    afterEach { unmockkAll() }

    describe("currentWeapon") {
        it("gives the handler the old weapon as previous, and null on the first set") {
            megaman.currentWeapon = MegamanWeapons.MEGA_BUSTER
            megaman.currentWeapon = MegamanWeapons.FRIGID_SHOT

            megaman.currentWeapon shouldBe MegamanWeapons.FRIGID_SHOT
            verifyOrder {
                handler.onChangeWeapon(MegamanWeapons.MEGA_BUSTER, null)
                handler.onChangeWeapon(MegamanWeapons.FRIGID_SHOT, MegamanWeapons.MEGA_BUSTER)
            }
        }

        it("calls the handler before it stores the new weapon") {
            megaman.currentWeapon = MegamanWeapons.MEGA_BUSTER

            val seen = ArrayList<WeaponDef<*>>()
            every { handler.onChangeWeapon(any(), any()) } answers { seen.add(megaman.currentWeapon) }

            megaman.currentWeapon = MegamanWeapons.FRIGID_SHOT

            seen shouldBe listOf(MegamanWeapons.MEGA_BUSTER)
            megaman.currentWeapon shouldBe MegamanWeapons.FRIGID_SHOT
        }
    }
})
