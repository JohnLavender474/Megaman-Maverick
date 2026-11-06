package com.megaman.maverick.game.entities.state

import com.megaman.maverick.game.entities.bosses.GlacierMan
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class BossComponentTest : DescribeSpec({

    describe("BossComponent") {
        it("starts in the entrance phase") {
            BossComponent().phase shouldBe BossPhase.ENTRANCE
        }

        it("goes back to the entrance phase on reset") {
            val component = BossComponent()
            component.phase = BossPhase.FIGHT

            component.reset()

            component.phase shouldBe BossPhase.ENTRANCE
        }

        it("exists on a boss that has not spawned yet, in the entrance phase") {
            val boss = GlacierMan(testServices())

            boss.initialized shouldBe false
            boss.bossComponent.phase shouldBe BossPhase.ENTRANCE
        }
    }
})
