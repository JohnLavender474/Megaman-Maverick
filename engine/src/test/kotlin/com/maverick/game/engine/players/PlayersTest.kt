package com.maverick.game.engine.players

import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.entities.IGameEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private class TestView(override val slot: PlayerSlot, override val entity: IGameEntity) : PlayerView {
    var score = 0f
}

private fun IPlayers<TestView>.totalScore(): Float {
    var total = 0f
    forEach { total += it.score }
    return total
}

class PlayersTest : DescribeSpec({

    fun players(capacity: Int = 3) = Players(capacity, ::TestView)

    fun spawnedEntity() = MockGameEntity().also { it.spawned = true }

    describe("Players") {
        it("fails for a capacity below 1") {
            shouldThrow<IllegalArgumentException> { players(0) }
        }

        it("registers a player and gives its view") {
            val players = players()
            val entity = spawnedEntity()

            val view = players.register(PlayerSlot(1), entity)

            players.get(PlayerSlot(1)) shouldBeSameInstanceAs view
            view.slot shouldBe PlayerSlot(1)
            view.entity shouldBeSameInstanceAs entity
        }

        it("gives null for an empty slot and for a slot out of range") {
            val players = players()

            players.get(PlayerSlot(0)) shouldBe null
            players.get(PlayerSlot(3)) shouldBe null
            players.get(PlayerSlot(-1)) shouldBe null
        }

        it("fails to register in a slot that already has a player") {
            val players = players()
            players.register(PlayerSlot(0), spawnedEntity())

            shouldThrow<IllegalStateException> { players.register(PlayerSlot(0), spawnedEntity()) }
        }

        it("fails to register in a slot out of range") {
            shouldThrow<IllegalArgumentException> { players().register(PlayerSlot(3), spawnedEntity()) }
        }

        it("unregisters a player") {
            val players = players()
            players.register(PlayerSlot(0), spawnedEntity())

            players.unregister(PlayerSlot(0))

            players.get(PlayerSlot(0)) shouldBe null
        }
    }

    describe("helpers") {
        it("forEach visits the players in slot order and skips empty slots") {
            val players = players()
            players.register(PlayerSlot(2), spawnedEntity())
            players.register(PlayerSlot(0), spawnedEntity())

            val visited = mutableListOf<Int>()
            players.forEach { visited.add(it.slot.index) }

            visited shouldBe listOf(0, 2)
        }

        it("forEachPresent skips players that are not present") {
            val players = players()
            players.register(PlayerSlot(0), spawnedEntity())
            players.register(PlayerSlot(1), MockGameEntity())

            val visited = mutableListOf<Int>()
            players.forEachPresent { visited.add(it.slot.index) }

            visited shouldBe listOf(0)
        }

        it("forEach does not filter by present") {
            val players = players()
            players.register(PlayerSlot(0), MockGameEntity())

            var count = 0
            players.forEach { count++ }

            count shouldBe 1
        }

        it("any, all and firstOrNull test the registered players in slot order") {
            val players = players()
            players.register(PlayerSlot(0), spawnedEntity()).score = 1f
            players.register(PlayerSlot(2), spawnedEntity()).score = 5f

            players.any { it.score > 4f } shouldBe true
            players.any { it.score > 9f } shouldBe false
            players.all { it.score > 0f } shouldBe true
            players.all { it.score > 2f } shouldBe false
            players.firstOrNull { it.score > 0f }!!.slot shouldBe PlayerSlot(0)
            players.firstOrNull { it.score > 9f } shouldBe null
        }

        it("all is true when no player is registered") {
            players().all { false } shouldBe true
        }

        it("minByOrNull gives the lowest value and a tie goes to the lower slot") {
            val players = players()
            players.register(PlayerSlot(0), spawnedEntity()).score = 3f
            players.register(PlayerSlot(1), spawnedEntity()).score = 2f
            players.register(PlayerSlot(2), spawnedEntity()).score = 2f

            players.minByOrNull { it.score }!!.slot shouldBe PlayerSlot(1)
        }

        it("minByOrNull gives null when no player is registered") {
            players().minByOrNull { it.score } shouldBe null
        }

        it("slotOf gives the slot of a registered entity, or null") {
            val players = players()
            val entity = spawnedEntity()
            players.register(PlayerSlot(2), entity)

            players.slotOf(entity) shouldBe PlayerSlot(2)
            players.slotOf(spawnedEntity()) shouldBe null
        }

        it("registeredSlots adds the registered slots in slot order") {
            val players = players()
            players.register(PlayerSlot(2), spawnedEntity())
            players.register(PlayerSlot(0), MockGameEntity())

            players.registeredSlots().toList() shouldBe listOf(PlayerSlot(0), PlayerSlot(2))
        }

        it("a game adds its own helper as an extension") {
            val players = players()
            players.register(PlayerSlot(0), spawnedEntity()).score = 1.5f
            players.register(PlayerSlot(1), spawnedEntity()).score = 2.5f

            players.totalScore() shouldBe 4f
        }
    }
})
