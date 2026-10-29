package com.megaman.maverick.game.screens.levels

import com.maverick.game.engine.events.Event
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.ScreenEnum
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify

class MegaLevelScreenEventsTest : DescribeSpec({

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
