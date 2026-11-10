package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.megaman.handlers.MegamanWeaponsHandler
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.state.FreezeComponent
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.TagsComponent
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify

class MegamanDeathCleanupTest : DescribeSpec({

    afterEach {
        MegaEntityFactory.reset()
        unmockkAll()
    }

    describe("the death of Megaman") {
        it("resets the weapons handler, so the spawned sets and the cooldowns do not survive the death") {
            val services = testServices(engine = mockk<GameEngine>(relaxed = true))
            val megaman = Megaman(services)
            megaman.addComponent(TagsComponent(Megaman.TAG))
            services.players.register(PlayerSlot(0), megaman)
            val handler = mockk<MegamanWeaponsHandler>(relaxed = true)
            megaman.weaponsHandler = handler

            megaman.onDestroy()

            verify(exactly = 1) { handler.reset() }
        }
    }

    describe("the axe throw") {
        fun throwAxe(state: EntityExistenceState): GameEngine {
            val engine = mockk<GameEngine>(relaxed = true)
            val gameState = GameState().also { it.addWeapon(MegamanWeapon.AXE_SWINGER) }
            val services = testServices(engine = engine, gameState = gameState)
            MegaEntityFactory.init(services)
            val megaman = mockk<Megaman>(relaxed = true)
            every { megaman.services } returns services
            every { megaman.existenceState } returns state
            every { megaman.getComponent(PlayPhaseComponent::class) } returns PlayPhaseComponent(PlayPhase.IN_PLAY)
            every { megaman.getComponent(FreezeComponent::class) } returns null
            val handler = MegamanWeaponsHandler(megaman).also { it.reset() }

            handler.fireWeapon(MegamanWeapon.AXE_SWINGER, MegaChargeStatus.NOT_CHARGED) shouldBe true
            if (state == EntityExistenceState.LIVE) {
                services.updatables.values().forEach { it.update(0.2f) }
            } else {
                services.updatables.values().forEach { it.update(0.05f) }
                every { megaman.existenceState } returns EntityExistenceState.DESTROY_QUEUED
                services.updatables.values().forEach { it.update(0.15f) }
            }
            return engine
        }

        it("throws the axe after the delay while the player lives") {
            val engine = throwAxe(EntityExistenceState.LIVE)
            verify(exactly = 1) { engine.spawn(ofType<Axe>(), any()) }
        }

        it("throws no axe when the player dies before the delay ends") {
            val engine = throwAxe(EntityExistenceState.DESTROY_QUEUED)
            verify(exactly = 0) { engine.spawn(ofType<Axe>(), any()) }
        }
    }
})
