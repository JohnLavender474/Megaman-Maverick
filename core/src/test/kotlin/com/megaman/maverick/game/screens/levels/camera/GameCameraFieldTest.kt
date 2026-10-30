package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.common.enums.Direction
import com.megaman.maverick.game.MegamanMaverickGame
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class GameCameraFieldTest : DescribeSpec({

    describe("the game camera field") {
        it("exists when the game is made and is the same instance on each read") {
            val game = MegamanMaverickGame()
            game.gameCamera shouldBeSameInstanceAs game.gameCamera
        }

        it("starts with direction UP") {
            MegamanMaverickGame().gameCamera.direction shouldBe Direction.UP
        }
    }
})
