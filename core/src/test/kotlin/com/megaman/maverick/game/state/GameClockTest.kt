package com.megaman.maverick.game.state

import com.megaman.maverick.game.MegamanMaverickGame
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GameClockTest : DescribeSpec({

    describe("GameClock") {
        it("starts not paused") {
            GameClock().paused shouldBe false
        }
    }

    describe("the pause and resume actions of the game") {
        it("set and clear the pause flag of the game clock") {
            val game = MegamanMaverickGame()
            game.clock.paused shouldBe false

            game.pause()
            game.clock.paused shouldBe true

            game.pause()
            game.clock.paused shouldBe true

            game.resume()
            game.clock.paused shouldBe false

            game.resume()
            game.clock.paused shouldBe false
        }
    }
})
