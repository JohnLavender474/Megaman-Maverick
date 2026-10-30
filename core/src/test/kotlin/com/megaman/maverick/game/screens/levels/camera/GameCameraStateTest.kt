package com.megaman.maverick.game.screens.levels.camera

import com.megaman.maverick.game.MegamanMaverickGame
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class GameCameraStateTest : DescribeSpec({

    describe("GameCameraState") {
        it("starts with rotating false") {
            val camera = GameCameraState()
            camera.rotating shouldBe false
        }

        it("keeps the values that are set") {
            val camera = GameCameraState()
            camera.rotating = true
            camera.rotating shouldBe true
        }
    }

    describe("the game") {
        it("holds one GameCameraState for the session, with rotating false at the start") {
            val game = MegamanMaverickGame()
            game.camera shouldBeSameInstanceAs game.camera
            game.camera.rotating shouldBe false
        }
    }
})
