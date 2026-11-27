package com.megaman.maverick.game.entities.megaman.handlers

import com.maverick.game.engine.components.IGameComponent
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.weapons.MEGAMAN_LOADOUT
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.players.PlayerCharacter
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.reflect.KClass

class MegamanWeaponChangeRushTest : DescribeSpec({

    lateinit var rushHandler: MegamanRushHandler
    lateinit var weaponsHandler: MegamanWeaponsHandler

    beforeEach {
        val state = GameState()
        MEGAMAN_LOADOUT.weapons.forEach { state.addWeapon(PlayerCharacter.MEGAMAN, it) }
        rushHandler = mockk(relaxed = true)
        val megaman = mockk<Megaman>(relaxed = true)
        every { megaman.services } returns testServices(gameState = state)
        every { megaman.character } returns PlayerCharacter.MEGAMAN
        every { megaman.getComponent(any<KClass<IGameComponent>>()) } returns null
        every { megaman.rushHandler } returns rushHandler
        weaponsHandler = MegamanWeaponsHandler(megaman)
    }

    afterEach { unmockkAll() }

    describe("a weapon change") {
        it("dismisses Rush when Megaman leaves a Rush ability") {
            weaponsHandler.onChangeWeapon(MegamanWeapons.MEGA_BUSTER, MegamanWeapons.RUSH_JET)
            verify(exactly = 1) { rushHandler.dismiss() }
        }

        it("keeps Rush when Megaman selects the same Rush ability again") {
            weaponsHandler.onChangeWeapon(MegamanWeapons.RUSH_JET, MegamanWeapons.RUSH_JET)
            verify(exactly = 0) { rushHandler.dismiss() }
        }

        it("does not touch Rush when the old weapon is not a Rush ability") {
            weaponsHandler.onChangeWeapon(MegamanWeapons.RUSH_JET, MegamanWeapons.MEGA_BUSTER)
            verify(exactly = 0) { rushHandler.dismiss() }
        }
    }
})
