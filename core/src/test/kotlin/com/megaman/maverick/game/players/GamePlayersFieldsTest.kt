package com.megaman.maverick.game.players

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.mockk

private class FieldsTestPlayerEntity(services: MegaServices) : MegaGameEntity(services), IDirectional {
    override var direction = Direction.UP
    override fun getType() = EntityType.MEGAMAN
}

class GamePlayersFieldsTest : DescribeSpec({

    describe("the game") {
        it("has players and gravity before create(), one object each for the session") {
            val game = MegamanMaverickGame()

            game.players shouldBeSameInstanceAs game.players
            game.gravity shouldBeSameInstanceAs game.gravity
            game.players.capacity shouldBe 1
            game.gravity.direction shouldBe Direction.UP
        }

        it("has a gravity that sets the direction of the players registered in its players") {
            val game = MegamanMaverickGame()
            val entity = FieldsTestPlayerEntity(testServices(engine = mockk<GameEngine>(relaxed = true)))
            game.players.register(PlayerSlot(0), entity)

            game.gravity.set(Direction.DOWN)

            game.gravity.direction shouldBe Direction.DOWN
            entity.direction shouldBe Direction.DOWN
        }
    }
})
