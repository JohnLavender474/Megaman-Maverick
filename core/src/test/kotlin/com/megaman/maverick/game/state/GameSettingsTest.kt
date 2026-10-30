package com.megaman.maverick.game.state

import com.megaman.maverick.game.MegamanMaverickGame
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class GameSettingsTest : DescribeSpec({

    describe("GameSettings") {
        it("has no performance before the game sets it, as the launch params had none") {
            shouldThrow<UninitializedPropertyAccessException> { GameSettings().performance }
        }

        it("keeps the performance that is set") {
            val settings = GameSettings()
            settings.performance = Performance.LOW
            settings.performance shouldBe Performance.LOW
        }
    }

    describe("the game") {
        it("holds one GameSettings for the session") {
            val game = MegamanMaverickGame()
            game.settings shouldBeSameInstanceAs game.settings
        }
    }

    describe("Performance") {
        it("keeps the values of the old nested enum") {
            Performance.entries.map { Triple(it.name, it.fps, it.fixedStep) } shouldBe listOf(
                Triple("VERY_LOW", 30, 1f / 75f),
                Triple("LOW", 45, 1f / 112.5f),
                Triple("MEDIUM", 60, 1f / 150f),
                Triple("HIGH", 90, 1f / 225f)
            )
        }
    }
})
