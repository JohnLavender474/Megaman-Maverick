package com.megaman.maverick.game.screens.levels

import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.events.Event
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.fights.FightPlans
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.levels.stats.BossHealthHandler
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

private fun boss() = mockk<AbstractBoss>(relaxed = true).also {
    every { it.existenceState } returns EntityExistenceState.LIVE
}

private fun gameWithLivePlayer() = mockk<MegamanMaverickGame>(relaxed = true).also {
    val player = mockk<MegaGameEntity>(relaxed = true)
    every { player.existenceState } returns EntityExistenceState.LIVE
    every { it.players.get(any()) } returns player
}

private fun MegaLevelScreen.ownFight(boss: AbstractBoss) =
    fights.start(
        boss, FightPlans.find("BigAssMaverickRobot", Properties()), testServices(), mockk<BossHealthHandler>(relaxed = true)
    ) { null }

class MegaLevelScreenEventsTest : DescribeSpec({

    describe("MegaLevelScreen and the fight") {
        it("reset cancels a fight in progress, as on a game over") {
            val screen = MegaLevelScreen(gameWithLivePlayer())
            screen.ownFight(boss())

            runCatching { screen.reset() }

            screen.fights.active shouldBe false
        }
    }

    describe("MegaLevelScreen and the fight events") {
        it("listens to the room entry and the victory, and to no event that a fight handles") {
            val mask = MegaLevelScreen(mockk<MegamanMaverickGame>(relaxed = true)).eventKeyMask

            mask.contains(EventType.ENTER_BOSS_ROOM) shouldBe true
            mask.contains(EventType.VICTORY_EVENT) shouldBe true
            listOf(EventType.END_BOSS_SPAWN, EventType.BOSS_DEFEATED, EventType.UNLOCK_GATE)
                .forEach { mask.contains(it) shouldBe false }
        }
    }

    describe("MegaLevelScreen and the respawn events") {
        it("listens to the death and to no event that the respawn routine sends") {
            val mask = MegaLevelScreen(mockk<MegamanMaverickGame>(relaxed = true)).eventKeyMask

            mask.contains(EventType.PLAYER_JUST_DIED) shouldBe true
            listOf(EventType.PLAYER_DONE_DYIN, EventType.PLAYER_READY, EventType.PLAYER_SPAWN)
                .forEach { mask.contains(it) shouldBe false }
        }
    }

    describe("MegaLevelScreen and GAME_COMPLETE") {
        it("listens to GAME_COMPLETE") {
            val screen = MegaLevelScreen(mockk<MegamanMaverickGame>(relaxed = true))

            screen.eventKeyMask.contains(EventType.GAME_COMPLETE) shouldBe true
        }

        it("switches to the ending screen once") {
            val game = mockk<MegamanMaverickGame>(relaxed = true)
            val screen = MegaLevelScreen(game)

            screen.onEvent(Event(EventType.GAME_COMPLETE))

            verify(exactly = 1) { game.setCurrentScreen(ScreenEnum.ENDING_SCREEN.name, any()) }
            verify(exactly = 1) { game.setCurrentScreen(any(), any()) }
        }
    }
})
