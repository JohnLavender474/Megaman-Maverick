package com.megaman.maverick.game.entities.megaman

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.locks.lock
import com.megaman.maverick.game.entities.megaman.components.defineFreezeComponent
import com.megaman.maverick.game.entities.megaman.components.defineStunComponent
import com.megaman.maverick.game.entities.state.FreezeComponent
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.StunComponent
import com.megaman.maverick.game.entities.state.frozen
import com.megaman.maverick.game.entities.state.setPlayPhase
import com.megaman.maverick.game.entities.state.stunned
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.unmockkAll

class MegamanInvincibleTest : DescribeSpec({

    lateinit var megaman: Megaman

    beforeEach {
        megaman = Megaman(testServices(engine = mockk<GameEngine>(relaxed = true)))
        megaman.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        megaman.addComponent(megaman.damageRecovery)
        megaman.addComponent(LocksComponent(MegaLockGroup.entries.size))
        megaman.addComponent(megaman.defineStunComponent())
        megaman.addComponent(megaman.defineFreezeComponent())
        megaman.existenceState = EntityExistenceState.LIVE
    }

    afterEach { unmockkAll() }

    describe("invincible") {
        it("is false for a live player in play") {
            megaman.invincible shouldBe false
        }

        it("is true during recovery") {
            megaman.damageRecovery.update(0f, damaged = false, damageJustFinished = true)

            megaman.invincible shouldBe true
        }

        it("is true with a damage lock") {
            megaman.lock(MegaLockGroup.DAMAGE, MegaLockReason.TELEPORT)

            megaman.invincible shouldBe true
        }

        it("is true before play and after play") {
            megaman.setPlayPhase(PlayPhase.PRE_PLAY)
            megaman.invincible shouldBe true

            megaman.setPlayPhase(PlayPhase.POST_PLAY)
            megaman.invincible shouldBe true
        }

        it("is true when the player does not stay in the engine") {
            megaman.existenceState = EntityExistenceState.UNSPAWNED

            megaman.invincible shouldBe true
        }

        it("is false while stunned") {
            megaman.getComponent(StunComponent::class)!!.start()

            megaman.stunned shouldBe true
            megaman.invincible shouldBe false
        }

        it("is false while frozen") {
            megaman.getComponent(FreezeComponent::class)!!.freeze()

            megaman.frozen shouldBe true
            megaman.invincible shouldBe false
        }
    }
})
