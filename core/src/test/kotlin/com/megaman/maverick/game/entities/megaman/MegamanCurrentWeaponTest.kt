package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.GameEngine
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
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
            megaman.currentWeapon = MegamanWeapon.MEGA_BUSTER
            megaman.currentWeapon = MegamanWeapon.FRIGID_SHOT

            megaman.currentWeapon shouldBe MegamanWeapon.FRIGID_SHOT
            verifyOrder {
                handler.onChangeWeapon(MegamanWeapon.MEGA_BUSTER, null)
                handler.onChangeWeapon(MegamanWeapon.FRIGID_SHOT, MegamanWeapon.MEGA_BUSTER)
            }
        }

        it("calls the handler before it stores the new weapon") {
            megaman.currentWeapon = MegamanWeapon.MEGA_BUSTER

            val seen = ArrayList<MegamanWeapon>()
            every { handler.onChangeWeapon(any(), any()) } answers { seen.add(megaman.currentWeapon) }

            megaman.currentWeapon = MegamanWeapon.FRIGID_SHOT

            seen shouldBe listOf(MegamanWeapon.MEGA_BUSTER)
            megaman.currentWeapon shouldBe MegamanWeapon.FRIGID_SHOT
        }
    }
})
