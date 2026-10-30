package com.megaman.maverick.game.screens.levels.camera

import com.megaman.maverick.game.MegamanMaverickGame
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class GameCameraStateTest : DescribeSpec({

    describe("GameCameraState") {
        it("starts with both flags false") {
            val camera = GameCameraState()
            camera.rotating shouldBe false
            camera.focusSnappedAway shouldBe false
        }

        it("keeps the values that are set") {
            val camera = GameCameraState()
            camera.rotating = true
            camera.focusSnappedAway = true
            camera.rotating shouldBe true
            camera.focusSnappedAway shouldBe true
        }
    }

    describe("the game") {
        it("holds one GameCameraState for the session, with both flags false at the start") {
            val game = MegamanMaverickGame()
            game.camera shouldBeSameInstanceAs game.camera
            game.camera.rotating shouldBe false
            game.camera.focusSnappedAway shouldBe false
        }
    }
})
