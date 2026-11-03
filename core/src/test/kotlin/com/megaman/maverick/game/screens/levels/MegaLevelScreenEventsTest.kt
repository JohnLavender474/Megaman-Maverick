package com.megaman.maverick.game.screens.levels

import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
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

private fun boss(mini: Boolean) = mockk<AbstractBoss>(relaxed = true).also {
    every { it.mini } returns mini
    every { it.existenceState } returns EntityExistenceState.LIVE
    every { it.getEventOnDefeated() } returns EventType.VICTORY_EVENT
}

private fun gameWithLivePlayer() = mockk<MegamanMaverickGame>(relaxed = true).also {
    val player = mockk<MegaGameEntity>(relaxed = true)
    every { player.existenceState } returns EntityExistenceState.LIVE
    every { it.players.get(any()) } returns player
}

private fun MegaLevelScreen.ownFight(boss: AbstractBoss) =
    fights.start(
        boss, FightPlans.find("BigAssMaverickRobot", Properties())!!, testServices(), mockk<BossHealthHandler>(relaxed = true)
    ) { null }

private fun bossEvent(key: EventType, boss: AbstractBoss) = Event(key, props(ConstKeys.BOSS pairTo boss))

private fun MegamanMaverickGame.submitted(key: EventType, times: Int) =
    verify(exactly = times) { events.submitEvent(match { it.key == key }) }

class MegaLevelScreenEventsTest : DescribeSpec({

    describe("MegaLevelScreen boss branches and the fight route") {
        it("BOSS_READY acts for a boss with no fight") {
            val game = gameWithLivePlayer()

            MegaLevelScreen(game).onEvent(bossEvent(EventType.BOSS_READY, boss(mini = true)))

            game.submitted(EventType.END_BOSS_SPAWN, 1)
        }

        it("BOSS_READY skips a boss that a fight owns") {
            val game = gameWithLivePlayer()
            val screen = MegaLevelScreen(game)
            val boss = boss(mini = true)
            screen.ownFight(boss)

            screen.onEvent(bossEvent(EventType.BOSS_READY, boss))

            game.submitted(EventType.END_BOSS_SPAWN, 0)
        }

        it("END_BOSS_SPAWN acts with no fight, and skips during a fight") {
            val game = gameWithLivePlayer()
            val screen = MegaLevelScreen(game)
            screen.ownFight(boss(mini = false))

            screen.onEvent(Event(EventType.END_BOSS_SPAWN))
            game.submitted(EventType.TURN_CONTROLLER_ON, 0)

            screen.fights.cancel()
            screen.onEvent(Event(EventType.END_BOSS_SPAWN))
            game.submitted(EventType.TURN_CONTROLLER_ON, 1)
        }

        it("BOSS_DEFEATED acts for a boss with no fight") {
            val game = gameWithLivePlayer()

            runCatching {
                MegaLevelScreen(game).onEvent(bossEvent(EventType.BOSS_DEFEATED, boss(mini = false)))
            }

            game.submitted(EventType.TURN_CONTROLLER_OFF, 1)
        }

        it("BOSS_DEFEATED skips a boss that a fight owns") {
            val game = gameWithLivePlayer()
            val screen = MegaLevelScreen(game)
            val boss = boss(mini = false)
            screen.ownFight(boss)

            screen.onEvent(bossEvent(EventType.BOSS_DEFEATED, boss))

            game.submitted(EventType.TURN_CONTROLLER_OFF, 0)
        }

        it("BOSS_DEAD acts for a boss with no fight") {
            val game = gameWithLivePlayer()

            MegaLevelScreen(game).onEvent(bossEvent(EventType.BOSS_DEAD, boss(mini = false)))

            game.submitted(EventType.VICTORY_EVENT, 1)
        }

        it("INTERMEDIATE_BOSS_DEAD acts for a boss with no fight") {
            val game = gameWithLivePlayer()

            runCatching {
                MegaLevelScreen(game).onEvent(bossEvent(EventType.INTERMEDIATE_BOSS_DEAD, boss(mini = false)))
            }

            game.submitted(EventType.TURN_CONTROLLER_ON, 1)
        }

        it("INTERMEDIATE_BOSS_DEAD skips a boss that a fight owns") {
            val game = gameWithLivePlayer()
            val screen = MegaLevelScreen(game)
            val boss = boss(mini = false)
            screen.ownFight(boss)

            screen.onEvent(bossEvent(EventType.INTERMEDIATE_BOSS_DEAD, boss))

            game.submitted(EventType.TURN_CONTROLLER_ON, 0)
            verify(exactly = 0) { game.audio.playMusic(any(), any()) }
        }

        it("reset cancels a fight in progress, as on a game over") {
            val screen = MegaLevelScreen(gameWithLivePlayer())
            screen.ownFight(boss(mini = false))

            runCatching { screen.reset() }

            screen.fights.active shouldBe false
        }

        it("BOSS_DEAD skips a boss that a fight owns") {
            val game = gameWithLivePlayer()
            val screen = MegaLevelScreen(game)
            val boss = boss(mini = false)
            screen.ownFight(boss)

            screen.onEvent(bossEvent(EventType.BOSS_DEAD, boss))

            game.submitted(EventType.VICTORY_EVENT, 0)
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
