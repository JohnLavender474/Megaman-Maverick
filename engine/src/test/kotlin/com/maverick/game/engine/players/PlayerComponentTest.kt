package com.maverick.game.engine.players

import com.maverick.game.engine.MockGameEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PlayerComponentTest : DescribeSpec({

    describe("PlayerComponent") {
        it("gives the slot and marks the entity as a player") {
            val entity = MockGameEntity().also { it.addComponent(PlayerComponent(PlayerSlot(1))) }

            entity.playerSlot shouldBe PlayerSlot(1)
            entity.isPlayer shouldBe true
        }

        it("gives no slot for an entity without it") {
            val entity = MockGameEntity()

            entity.playerSlot shouldBe null
            entity.isPlayer shouldBe false
        }
    }
})
