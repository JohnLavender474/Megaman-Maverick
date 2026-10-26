package com.maverick.game.engine.players

import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.GameEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.mockk

private class ScoredEntity : GameEntity(mockk(relaxed = true)) {
    var score = 0f
    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

private fun IPlayers<ScoredEntity>.totalScore(): Float {
    var total = 0f
    forEach { total += it.score }
    return total
}

class PlayersTest : DescribeSpec({

    fun players(capacity: Int = 3) = Players<ScoredEntity>(capacity)

    describe("Players") {
        it("fails for a capacity below 1") {
            shouldThrow<IllegalArgumentException> { players(0) }
        }

        it("registers an entity, gives it back, and marks it with its slot") {
            val players = players()
            val entity = ScoredEntity()

            players.register(PlayerSlot(1), entity) shouldBeSameInstanceAs entity

            players.get(PlayerSlot(1)) shouldBeSameInstanceAs entity
            entity.playerSlot shouldBe PlayerSlot(1)
            entity.isPlayer shouldBe true
        }

        it("gives null for an empty slot and for a slot out of range") {
            val players = players()

            players.get(PlayerSlot(0)) shouldBe null
            players.get(PlayerSlot(3)) shouldBe null
            players.get(PlayerSlot(-1)) shouldBe null
        }

        it("fails to register in a slot that already has a player") {
            val players = players()
            players.register(PlayerSlot(0), ScoredEntity())

            shouldThrow<IllegalStateException> { players.register(PlayerSlot(0), ScoredEntity()) }
        }

        it("fails to register an entity that is already a player, and changes no slot") {
            val players = players()
            val entity = players.register(PlayerSlot(0), ScoredEntity())

            shouldThrow<IllegalStateException> { players.register(PlayerSlot(1), entity) }

            players.get(PlayerSlot(0)) shouldBeSameInstanceAs entity
            players.get(PlayerSlot(1)) shouldBe null
            entity.playerSlot shouldBe PlayerSlot(0)
        }

        it("fails to register in a slot out of range") {
            shouldThrow<IllegalArgumentException> { players().register(PlayerSlot(3), ScoredEntity()) }
        }

        it("unregisters a player and removes its player component") {
            val players = players()
            val entity = ScoredEntity()
            players.register(PlayerSlot(0), entity)

            players.unregister(PlayerSlot(0))

            players.get(PlayerSlot(0)) shouldBe null
            entity.isPlayer shouldBe false
            entity.playerSlot shouldBe null
        }
    }

    describe("helpers") {
        it("forEach visits the players in slot order and skips empty slots") {
            val players = players()
            players.register(PlayerSlot(2), ScoredEntity())
            players.register(PlayerSlot(0), ScoredEntity())

            val visited = mutableListOf<Int>()
            players.forEach { visited.add(it.playerSlot!!.index) }

            visited shouldBe listOf(0, 2)
        }

        it("forEach does not filter by spawned") {
            val players = players()
            players.register(PlayerSlot(0), ScoredEntity().also { it.spawned = false })

            var count = 0
            players.forEach { count++ }

            count shouldBe 1
        }

        it("any and all test the registered players") {
            val players = players()
            players.register(PlayerSlot(0), ScoredEntity()).score = 1f
            players.register(PlayerSlot(2), ScoredEntity()).score = 5f

            players.any { it.score > 4f } shouldBe true
            players.any { it.score > 9f } shouldBe false
            players.all { it.score > 0f } shouldBe true
            players.all { it.score > 2f } shouldBe false
        }

        it("all is true when no player is registered") {
            players().all { false } shouldBe true
        }

        it("registeredSlots adds the registered slots in slot order") {
            val players = players()
            players.register(PlayerSlot(2), ScoredEntity())
            players.register(PlayerSlot(0), ScoredEntity())

            players.registeredSlots().toList() shouldBe listOf(PlayerSlot(0), PlayerSlot(2))
        }

        it("a game adds its own helper as an extension") {
            val players = players()
            players.register(PlayerSlot(0), ScoredEntity()).score = 1.5f
            players.register(PlayerSlot(1), ScoredEntity()).score = 2.5f

            players.totalScore() shouldBe 4f
        }
    }
})
